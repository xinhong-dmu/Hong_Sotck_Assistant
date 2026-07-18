package com.hong.xin.stock.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.Stock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StockListCache {

    private static final String TAG = "StockListCache";
    private static final String FILE_NAME = "stock_list_cache.json";
    private static StockListCache instance;

    private final File cacheFile;
    private final Gson gson;

    private StockListCache(Context context) {
        gson = new Gson();
        cacheFile = new File(context.getFilesDir(), FILE_NAME);
    }

    public static synchronized StockListCache getInstance(Context context) {
        if (instance == null) {
            instance = new StockListCache(context.getApplicationContext());
        }
        return instance;
    }

    public synchronized List<Stock> load() {
        try {
            if (!cacheFile.exists() || cacheFile.length() == 0) return null;
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(cacheFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            Type type = new TypeToken<List<Stock>>() {}.getType();
            List<Stock> list = gson.fromJson(sb.toString(), type);
            Log.d(TAG, "load OK: size=" + (list != null ? list.size() : 0));
            return list != null && !list.isEmpty() ? list : null;
        } catch (Exception e) {
            Log.e(TAG, "load error: " + e.getMessage());
            return null;
        }
    }

    public synchronized void save(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) return;
        try {
            String json = gson.toJson(stocks);
            BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile));
            writer.write(json);
            writer.close();
            Log.d(TAG, "save OK: size=" + stocks.size());
        } catch (Exception e) {
            Log.e(TAG, "save error: " + e.getMessage());
        }
    }
}
