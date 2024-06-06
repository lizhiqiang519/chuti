package com.tencent.wxcloudrun.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.tencent.wxcloudrun.model.LinkUnhandleQuestion;
import com.tencent.wxcloudrun.service.LinkUnhandleQuestionService;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
//@RequestMapping("/api")
public class XunfeiApi {
    private static final String hostUrl = "https://spark-api.xf-yun.com/v3.5/chat";
    private static final String appid = "1297070f";
    private static final String apiSecret = "ZWVmOGQyZGY1N2QxNTViM2IzNjE1ZTgx";
    private static final String apiKey = "d3c19ecd65641fee8bf220e67f76a5a2";

    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final Gson gson = new Gson();

    @Autowired
    private LinkUnhandleQuestionService linkUnhandleQuestionService;

    private static final Logger logger = LoggerFactory.getLogger(XunfeiApi.class);

    /**
     * 提交到AI生成题目。链接出题
     * @param requestBody
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/api/submitLinkChuti")
    @Async
    public CompletableFuture<String> getResponse(@RequestBody Map<String, String> requestBody) throws Exception {

        logger.info("getResponse入参:{}", requestBody);
        // 传进来的提示词
        String tishici = "根据链接内容出题，从专业老师的角度生成5道单选题（每道题必须包含七个字段：问题描述、选项A、选项B、选项C、选项D、答案、分析）：" + requestBody.get("tishici");
        String uuid = requestBody.get("uuid");

        if (tishici == null || tishici.isEmpty()) {
            return CompletableFuture.completedFuture("Error: tishici field is required.");
        }

        logger.info("开始处理请求，接收参数tishici: {}", tishici);
        logger.info("开始处理请求，接收参数uuid: {}", uuid);

        // 创建一个响应容器
        final StringBuilder responseContainer = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                try {
                    JSONObject requestJson = new JSONObject();
                    JSONObject header = new JSONObject();
                    header.put("app_id", appid);
                    header.put("uid", UUID.randomUUID().toString().substring(0, 10));

                    JSONObject parameter = new JSONObject();
                    JSONObject chat = new JSONObject();
                    chat.put("domain", "generalv3.5");
                    chat.put("temperature", 0.2);
                    chat.put("max_tokens", 8192);
                    parameter.put("chat", chat);

                    JSONObject payload = new JSONObject();
                    JSONObject message = new JSONObject();
                    JSONArray text = new JSONArray();

                    JSONObject userMessage = new JSONObject();
                    userMessage.put("role", "user");
                    userMessage.put("content", tishici);
                    text.add(userMessage);

                    message.put("text", text);
                    payload.put("message", message);

                    requestJson.put("header", header);
                    requestJson.put("parameter", parameter);
                    requestJson.put("payload", payload);

                    webSocket.send(requestJson.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Response: " + text);
                JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                List<Text> textList = myJsonParse.payload.choices.text;
                for (Text temp : textList) {
                    responseContainer.append(temp.getContent());
                }
                if (myJsonParse.header.status == 2) {
                    logger.info("最终结果：{}", responseContainer);

                    // 创建一个新的LinkUnhandleQuestion对象
                    LinkUnhandleQuestion linkUnhandleQuestion = new LinkUnhandleQuestion();
                    linkUnhandleQuestion.setUuid(uuid);
                    linkUnhandleQuestion.setTishici(tishici);
                    linkUnhandleQuestion.setResult(String.valueOf(responseContainer));
                    linkUnhandleQuestion.setLink(requestBody.get("tishici"));
                    // 新增记录
                    linkUnhandleQuestionService.addLinkUnhandleQuestion(linkUnhandleQuestion);

                    latch.countDown();
                    webSocket.close(1000, null);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
                latch.countDown();
            }
        };

        client.newWebSocket(request, listener);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                latch.await(100, TimeUnit.SECONDS); // 等待响应完成，最多等待100秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Error: Interrupted while waiting for response.";
            }

            if (responseContainer.length() == 0) {
                return "Error: No response received within the timeout period.";
            }

            return responseContainer.toString();
        });

        return future;
    }

    /**
     * 获取出题结果，链接出题
     * @param requestBody
     * @return
     */
    @PostMapping("/api/getLinkChutiResult")
    public LinkUnhandleQuestion getLinkResult(@RequestBody Map<String, String> requestBody) {

        String uuid = requestBody.get("uuid");
        logger.info("getLinkChutiResult入参：{}",uuid);
        LinkUnhandleQuestion retrievedQuestion = linkUnhandleQuestionService.getLinkUnhandleQuestionByUuid(uuid);
        logger.info("getLinkChutiResult结果：{}",retrievedQuestion.toString());

        return retrievedQuestion;
    }



//    @PostMapping(value = "/api/getResponse")
//    public String getResponse(@RequestBody Map<String, String> requestBody) throws Exception {
//
//        logger.info("getResponse入参:{}",requestBody);
//        //传进来的提示词
//        String tishici = "根据链接内容出题，从专业老师的角度生成5道单选题（每道题必须包含七个字段：问题描述、选项A、选项B、选项C、选项D、答案、分析）："+ requestBody.get("tishici") ;
//        String uuid =  requestBody.get("uuid");
//
//        if (tishici == null || tishici.isEmpty()) {
//            return "Error: tishici field is required.";
//        }
//
//        logger.info("开始处理请求，接收参数tishici: {}", tishici);
//        logger.info("开始处理请求，接收参数uuid: {}", uuid);
//
//        // 根据UUID查询记录
//        //LinkUnhandleQuestion retrievedQuestion = linkUnhandleQuestionService.getLinkUnhandleQuestionByUuid(uuid);
//
//        final StringBuilder responseContainer = new StringBuilder();
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
//        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
//        Request request = new Request.Builder().url(url).build();
//
//        WebSocketListener listener = new WebSocketListener() {
//            @Override
//            public void onOpen(WebSocket webSocket, Response response) {
//                try {
//                    JSONObject requestJson = new JSONObject();
//                    JSONObject header = new JSONObject();
//                    header.put("app_id", appid);
//                    header.put("uid", UUID.randomUUID().toString().substring(0, 10));
//
//                    JSONObject parameter = new JSONObject();
//                    JSONObject chat = new JSONObject();
//                    chat.put("domain", "generalv3.5");
//                    chat.put("temperature", 0.2);
//                    chat.put("max_tokens", 8192);
////                    chat.put("streaming", false);
//                    parameter.put("chat", chat);
//
//                    JSONObject payload = new JSONObject();
//                    JSONObject message = new JSONObject();
//                    JSONArray text = new JSONArray();
//
//                    JSONObject userMessage = new JSONObject();
//                    userMessage.put("role", "user");
//                    userMessage.put("content", tishici);
//                    text.add(userMessage);
//
//                    message.put("text", text);
//                    payload.put("message", message);
//
//                    requestJson.put("header", header);
//                    requestJson.put("parameter", parameter);
//                    requestJson.put("payload", payload);
//
//                    webSocket.send(requestJson.toString());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    latch.countDown();
//                }
//            }
//
//            @Override
//            public void onMessage(WebSocket webSocket, String text) {
//                System.out.println("Response: " + text);
//                JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
//                List<Text> textList = myJsonParse.payload.choices.text;
//                for (Text temp : textList) {
//                    responseContainer.append(temp.getContent());
//                }
//                if (myJsonParse.header.status == 2) {
//                    logger.info("最终结果：{}",responseContainer);
//
//                    // 创建一个新的LinkUnhandleQuestion对象
//                    LinkUnhandleQuestion linkUnhandleQuestion = new LinkUnhandleQuestion();
//                    linkUnhandleQuestion.setUuid(uuid);
//                    linkUnhandleQuestion.setTishici(tishici);
//                    linkUnhandleQuestion.setResult(String.valueOf(responseContainer));
//                    linkUnhandleQuestion.setLink(requestBody.get("tishici"));
//                    // 新增记录
//                    linkUnhandleQuestionService.addLinkUnhandleQuestion(linkUnhandleQuestion);
//
//                    latch.countDown();
//                    webSocket.close(1000, null);
//                }
//            }
//
//            @Override
//            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
//                t.printStackTrace();
//                latch.countDown();
//            }
//        };
//
//        client.newWebSocket(request, listener);
//
//        latch.await(100, TimeUnit.SECONDS); // 等待响应完成，最多等待120秒
//
//        if (responseContainer.length() == 0) {
//            return "Error: No response received within the timeout period.";
//        }
//
//        return responseContainer.toString();
//    }

    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        String preStr = "host: " + url.getHost() + "\n" + "date: " + date + "\n" + "GET " + url.getPath() + " HTTP/1.1";

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();

        return httpUrl.toString();
    }

    //返回的json结果拆解
    class JsonParse {
        Header header;
        Payload payload;
    }

    class Header {
        int code;
        int status;
        String sid;
    }

    class Payload {
        Choices choices;
    }

    class Choices {
        List<Text> text;
    }

    class Text {
        String role;
        String content;

        public Text(String content) {
            this.content = content;
        }

        // 假设的getContent方法，返回文本内容
        public String getContent() {
            return content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }


}
