package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.ChatMessage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatHistoryManager {

    private static final String PREF_NAME = "chat_history";
    private static final String KEY_HISTORY = "history";

    private static ChatHistoryManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, List<ChatMessage>> history;

    private ChatHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        history = loadHistory();
    }

    public static synchronized ChatHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private Map<String, List<ChatMessage>> loadHistory() {
        try {
            String json = prefs.getString(KEY_HISTORY, "{}");
            Type type = new TypeToken<Map<String, List<ChatMessage>>>() {}.getType();
            Map<String, List<ChatMessage>> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void saveHistory() {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }

    public List<ChatMessage> getMessages(String stockCode) {
        List<ChatMessage> msgs = history.get(stockCode);
        return msgs != null ? new ArrayList<>(msgs) : new ArrayList<>();
    }

    public void setMessages(String stockCode, List<ChatMessage> messages) {
        history.put(stockCode, new ArrayList<>(messages));
        saveHistory();
    }

    public void addMessage(String stockCode, ChatMessage message) {
        List<ChatMessage> msgs = history.get(stockCode);
        if (msgs == null) {
            msgs = new ArrayList<>();
            history.put(stockCode, msgs);
        }
        msgs.add(message);
        saveHistory();
    }

    public void clearHistory(String stockCode) {
        history.remove(stockCode);
        saveHistory();
    }
}
