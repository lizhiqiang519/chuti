package com.tencent.wxcloudrun.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import okhttp3.*;
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

@RestController
@RequestMapping("/api")
public class XunfeiApi {
    private static final String hostUrl = "https://spark-api.xf-yun.com/v3.5/chat";
    private static final String appid = "1297070f";
    private static final String apiSecret = "ZWVmOGQyZGY1N2QxNTViM2IzNjE1ZTgx";
    private static final String apiKey = "d3c19ecd65641fee8bf220e67f76a5a2";

    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final Gson gson = new Gson();

    @PostMapping("/getResponse")
    public String getResponse(@RequestBody Map<String, String> requestBody) throws Exception {
        String tishici = requestBody.get("tishici");

        if (tishici == null || tishici.isEmpty()) {
            return "Error: tishici field is required.";
        }

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
                    chat.put("domain", "generalv2");
                    chat.put("temperature", 0.5);
                    chat.put("max_tokens", 4096);
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
                    responseContainer.append(temp.content);
                }
                if (myJsonParse.header.status == 2) {
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

        latch.await(120, TimeUnit.SECONDS); // 等待响应完成，最多等待120秒

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
    }
}
