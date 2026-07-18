package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.Strategy;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StrategyManager {

    private static final String PREF_NAME = "strategies";
    private static final String KEY_STRATEGIES = "strategy_map";

    private static StrategyManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, Strategy> strategies;

    private StrategyManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        strategies = loadStrategies();
    }

    public static synchronized StrategyManager getInstance(Context context) {
        if (instance == null) {
            instance = new StrategyManager(context.getApplicationContext());
        }
        return instance;
    }

    private Map<String, Strategy> loadStrategies() {
        try {
            String json = prefs.getString(KEY_STRATEGIES, "{}");
            Type type = new TypeToken<Map<String, Strategy>>() {}.getType();
            Map<String, Strategy> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void saveStrategies() {
        prefs.edit().putString(KEY_STRATEGIES, gson.toJson(strategies)).apply();
    }

    public Strategy saveStrategy(Strategy strategy) {
        if (strategy.getId() == null || strategy.getId().isEmpty()) {
            strategy.setId(UUID.randomUUID().toString());
        }
        if (strategy.getCreatedAt() == 0) {
            strategy.setCreatedAt(System.currentTimeMillis());
        }
        strategies.put(strategy.getId(), strategy);
        saveStrategies();
        return strategy;
    }

    public void deleteStrategy(String id) {
        strategies.remove(id);
        saveStrategies();
    }

    public Strategy getStrategy(String id) {
        return strategies.get(id);
    }

    public List<Strategy> getStrategiesByStock(String stockCode) {
        List<Strategy> result = new ArrayList<>();
        for (Strategy s : strategies.values()) {
            if (s.getStockCode() != null && s.getStockCode().equals(stockCode)) {
                result.add(s);
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return result;
    }

    public List<Strategy> getAllStrategies() {
        List<Strategy> result = new ArrayList<>(strategies.values());
        Collections.sort(result, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return result;
    }

    public List<Strategy> getActiveStrategies() {
        List<Strategy> result = new ArrayList<>();
        for (Strategy s : strategies.values()) {
            if (s.isActive()) {
                result.add(s);
            }
        }
        return result;
    }

    public void toggleActive(String id, boolean active) {
        Strategy s = strategies.get(id);
        if (s != null) {
            s.setActive(active);
            saveStrategies();
        }
    }
}
