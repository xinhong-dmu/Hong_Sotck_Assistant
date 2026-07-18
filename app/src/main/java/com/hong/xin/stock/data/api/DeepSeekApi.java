package com.hong.xin.stock.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hong.xin.stock.data.model.ChatMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekApi {

    private static final String TAG = "DeepSeekApi";
    private static final String BASE_URL = "https://api.deepseek.com/chat/completions";
    private static final String PREF_NAME = "deepseek_config";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String DEFAULT_MODEL = "deepseek-v4-pro";
    private static final Gson GSON = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public static String getApiKey(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_KEY, "");
    }

    public static void setApiKey(Context context, String apiKey) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public static String getModel(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MODEL, DEFAULT_MODEL);
    }

    public static void setModel(Context context, String model) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODEL, model).apply();
    }

    public interface ChatCallback {
        void onResult(String content);
        void onError(String error);
    }

    public interface StreamChatCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(String error);
    }

    public static void chat(Context context, List<ChatMessage> messages, ChatCallback callback) {
        new Thread(() -> {
            try {
                String apiKey = getApiKey(context);
                if (apiKey.isEmpty()) {
                    callback.onError("请先设置API Key");
                    return;
                }

                JsonArray msgArray = new JsonArray();
                for (ChatMessage msg : messages) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("role", msg.getRole());
                    obj.addProperty("content", msg.getContent());
                    msgArray.add(obj);
                }

                JsonObject body = new JsonObject();
                body.addProperty("model", getModel(context));
                body.add("messages", msgArray);
                body.addProperty("stream", false);

                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                Response response = CLIENT.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "API error: " + response.code() + " " + respBody);
                    callback.onError("请求失败: " + response.code());
                    return;
                }

                JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    String content = message.get("content").getAsString();
                    callback.onResult(content);
                } else {
                    callback.onError("AI返回为空");
                }
            } catch (IOException e) {
                Log.e(TAG, "chat error: " + e.getMessage(), e);
                callback.onError("网络错误: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "chat error: " + e.getMessage(), e);
                callback.onError("请求失败: " + e.getMessage());
            }
        }).start();
    }

    public static void chatStream(Context context, List<ChatMessage> messages, StreamChatCallback callback) {
        new Thread(() -> {
            try {
                String apiKey = getApiKey(context);
                if (apiKey.isEmpty()) {
                    callback.onError("请先设置API Key");
                    return;
                }

                JsonArray msgArray = new JsonArray();
                for (ChatMessage msg : messages) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("role", msg.getRole());
                    obj.addProperty("content", msg.getContent());
                    msgArray.add(obj);
                }

                JsonObject body = new JsonObject();
                body.addProperty("model", getModel(context));
                body.add("messages", msgArray);
                body.addProperty("stream", true);

                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                Response response = CLIENT.newCall(request).execute();

                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    Log.e(TAG, "API error: " + response.code() + " " + errBody);
                    callback.onError("请求失败: " + response.code());
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), "UTF-8"));
                String line;
                StringBuilder contentBuilder = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        callback.onComplete();
                        return;
                    }

                    try {
                        JsonObject json = GSON.fromJson(data, JsonObject.class);
                        JsonArray choices = json.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                            if (delta != null && delta.has("content")) {
                                String chunk = delta.get("content").getAsString();
                                if (chunk != null && !chunk.isEmpty()) {
                                    callback.onChunk(chunk);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "parse chunk error: " + e.getMessage());
                    }
                }
                callback.onComplete();
            } catch (IOException e) {
                Log.e(TAG, "chatStream error: " + e.getMessage(), e);
                callback.onError("网络错误: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "chatStream error: " + e.getMessage(), e);
                callback.onError("请求失败: " + e.getMessage());
            }
        }).start();
    }
}
