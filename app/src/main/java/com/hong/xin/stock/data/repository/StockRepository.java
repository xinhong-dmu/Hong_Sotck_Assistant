package com.hong.xin.stock.data.repository;

import android.content.Context;
import android.util.Log;

import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.util.DebugLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockRepository {

    private static final String TAG = "StockRepository";
    private static StockRepository instance;
    private List<Stock> allStocks = new ArrayList<>();

    private StockRepository() {
    }

    public static StockRepository getInstance() {
        if (instance == null) {
            instance = new StockRepository();
        }
        return instance;
    }

    public void init(Context context) {
        allStocks.clear();
        try {
            InputStream is = context.getAssets().open("stock_list.csv");
            DebugLogger.i(TAG, "CSV file opened successfully");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, "UTF-8"));
            String line;
            int lineCount = 0;
            boolean isFirst = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isFirst) {
                    isFirst = false;
                    if (line.length() > 0 && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                    if (line.startsWith("code")) continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    String type = (parts.length >= 3) ? parts[2].trim() : Stock.TYPE_STOCK;
                    if (!code.isEmpty() && !name.isEmpty() && !code.equals("code")) {
                        allStocks.add(new Stock(code, name, type));
                        lineCount++;
                    }
                }
            }
            reader.close();
            DebugLogger.i(TAG, "CSV loaded: " + lineCount + " stocks");
        } catch (Exception e) {
            DebugLogger.e(TAG, "Failed to load stock_list.csv", e);
        }
    }

    public int getStockCount() {
        return allStocks.size();
    }

    public List<Stock> searchAll(String keyword) {
        return searchAll(keyword, null);
    }

    public List<Stock> searchAll(String keyword, String type) {
        List<Stock> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return result;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        for (Stock s : allStocks) {
            boolean matchType = (type == null) || type.equals(s.getType());
            if (matchType && (s.getCode().contains(kw) || s.getName().toLowerCase(Locale.ROOT).contains(kw))) {
                result.add(s);
            }
            if (result.size() >= 50) break;
        }
        return result;
    }
}
