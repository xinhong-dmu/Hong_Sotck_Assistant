package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PromptTemplateManager {

    private static final String PREF_NAME = "prompt_templates";
    private static final String KEY_TEMPLATES = "templates";

    private static PromptTemplateManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, String> templates;

    private PromptTemplateManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        templates = loadTemplates();
    }

    public static synchronized PromptTemplateManager getInstance(Context context) {
        if (instance == null) {
            instance = new PromptTemplateManager(context.getApplicationContext());
        }
        return instance;
    }

    private Map<String, String> loadTemplates() {
        try {
            String json = prefs.getString(KEY_TEMPLATES, "{}");
            Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
            Map<String, String> map = gson.fromJson(json, type);
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void saveTemplates() {
        prefs.edit().putString(KEY_TEMPLATES, gson.toJson(templates)).apply();
    }

    public Map<String, String> getTemplates() {
        return new LinkedHashMap<>(templates);
    }

    public String getTemplate(String name) {
        return templates.get(name);
    }

    public void saveTemplate(String name, String content) {
        templates.put(name, content);
        saveTemplates();
    }

    public void deleteTemplate(String name) {
        templates.remove(name);
        saveTemplates();
    }

    public List<String> getTemplateNames() {
        return new ArrayList<>(templates.keySet());
    }
}
