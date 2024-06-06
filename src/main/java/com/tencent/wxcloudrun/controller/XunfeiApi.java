package com.tencent.wxcloudrun.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tencent.wxcloudrun.service.LinkQuizQuestionService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private LinkQuizQuestionService linkQuizQuestionService;

    private static final Logger logger = LoggerFactory.getLogger(XunfeiApi.class);

    @PostMapping(value = "/api/getResponse")
    public String getResponse(@RequestBody Map<String, String> requestBody) throws Exception {

        //传进来的提示词
        String tishici = requestBody.get("tishici") ;
//                "。请按照结构返回结果：list结构，里面是每道题目，每道题目的属性有这些：linkQuestion（选择题问题描述）、linkOptionA（选项A）、linkOptionB（选项B）、" +
//                "linkOptionC（选项C）、linkOptionD（选项D）、linkAnswer（选择题答案）、linkExplanation（根据链接内容来回答的详细分析）、link（链接）、linkPrompt（提示词）、" +
//                "linkSummary（链接内容总结）";

        if (tishici == null || tishici.isEmpty()) {
            return "Error: tishici field is required.";
        }

        logger.info("开始处理请求，接收参数: {}", tishici);

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
//                    chat.put("streaming", false);
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
                    logger.info("最终结果：{}",responseContainer);

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

        latch.await(100, TimeUnit.SECONDS); // 等待响应完成，最多等待120秒

        if (responseContainer.length() == 0) {
            return "Error: No response received within the timeout period.";
        }

        return responseContainer.toString();
    }

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
