package com.hong.xin.stock.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekApi {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();
    private static final OkHttpClient CLIENT = HttpClientFactory.getClient();

    private static String apiKey = "";
    private static String model = "deepseek-chat";

    public static class ChatMessage {
        public final String role;
        public final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void setApiKey(String key) {
        apiKey = key;
    }

    public static boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public static void setModel(String m) {
        model = m;
    }

    public static void chat(List<ChatMessage> messages, Callback<String> callback) {
        if (!hasApiKey()) {
            callback.onResult("请先设置 API Key");
            return;
        }

        new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("model", model);
                body.addProperty("temperature", 0.7);
                body.addProperty("max_tokens", 4096);

                JsonArray msgArray = new JsonArray();
                for (ChatMessage msg : messages) {
                    JsonObject m = new JsonObject();
                    m.addProperty("role", msg.role);
                    m.addProperty("content", msg.content);
                    msgArray.add(m);
                }
                body.add("messages", msgArray);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(GSON.toJson(body), JSON))
                        .build();

                Response response = CLIENT.newCall(request).execute();
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    callback.onResult("API请求失败: HTTP " + response.code() + " - " + errorBody);
                    return;
                }

                String respBody = response.body() != null ? response.body().string() : "";
                JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                    String content = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .get("message").getAsJsonObject()
                            .get("content").getAsString();
                    callback.onResult(content);
                } else {
                    callback.onResult("API返回格式异常: " + respBody);
                }
            } catch (IOException e) {
                callback.onResult("网络请求失败: " + e.getMessage());
            } catch (Exception e) {
                callback.onResult("解析失败: " + e.getMessage());
            }
        }).start();
    }
}
