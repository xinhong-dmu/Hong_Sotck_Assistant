package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.Stock;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SelectedStockManager {

    private static final String TAG = "SelectedStockManager";
    private static final String PREF_NAME = "selected_stocks";
    private static final String KEY_STOCKS = "stocks";

    private static SelectedStockManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private List<Stock> selectedStocks;

    private SelectedStockManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        selectedStocks = loadStocks();
    }

    public static synchronized SelectedStockManager getInstance(Context context) {
        if (instance == null) {
            instance = new SelectedStockManager(context.getApplicationContext());
        }
        return instance;
    }

    private List<Stock> loadStocks() {
        try {
            String json = prefs.getString(KEY_STOCKS, "[]");
            Log.d(TAG, "loadStocks raw: " + json);
            if ("[]".equals(json)) {
                return new ArrayList<>();
            }
            Type type = new TypeToken<List<Stock>>() {}.getType();
            List<Stock> list = gson.fromJson(json, type);
            Log.d(TAG, "loadStocks OK: size=" + (list != null ? list.size() : 0));
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "loadStocks error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveStocks() {
        prefs.edit().putString(KEY_STOCKS, gson.toJson(selectedStocks)).apply();
    }

    public List<Stock> getSelectedStocks() {
        return new ArrayList<>(selectedStocks);
    }

    public boolean addStock(Stock stock) {
        for (Stock s : selectedStocks) {
            if (s.getCode().equals(stock.getCode())) {
                return false;
            }
        }
        selectedStocks.add(stock);
        saveStocks();
        return true;
    }

    public boolean removeStock(String code) {
        boolean removed = selectedStocks.removeIf(s -> s.getCode().equals(code));
        if (removed) {
            saveStocks();
        }
        return removed;
    }

    public boolean isSelected(String code) {
        for (Stock s : selectedStocks) {
            if (s.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
