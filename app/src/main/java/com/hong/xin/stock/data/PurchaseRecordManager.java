package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.PurchaseRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseRecordManager {

    private static final String PREF_NAME = "purchase_records_v2";
    private static final String KEY_RECORDS = "records";

    private static PurchaseRecordManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, List<PurchaseRecord>> records;

    private PurchaseRecordManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        records = loadRecords();
        migrateLegacyData(context);
    }

    public static synchronized PurchaseRecordManager getInstance(Context context) {
        if (instance == null) {
            instance = new PurchaseRecordManager(context.getApplicationContext());
        }
        return instance;
    }

    private Map<String, List<PurchaseRecord>> loadRecords() {
        try {
            String json = prefs.getString(KEY_RECORDS, "{}");
            Type type = new TypeToken<Map<String, List<PurchaseRecord>>>() {}.getType();
            Map<String, List<PurchaseRecord>> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void migrateLegacyData(Context context) {
        if (!records.isEmpty()) return;
        try {
            SharedPreferences oldPrefs = context.getSharedPreferences("purchase_records", Context.MODE_PRIVATE);
            String oldJson = oldPrefs.getString("records", "{}");
            if ("{}".equals(oldJson)) return;
            Type type = new TypeToken<Map<String, PurchaseRecord>>() {}.getType();
            Map<String, PurchaseRecord> oldMap = gson.fromJson(oldJson, type);
            if (oldMap == null || oldMap.isEmpty()) return;
            for (PurchaseRecord r : oldMap.values()) {
                if (r.getId() == null || r.getId().isEmpty()) {
                    r.setId(String.valueOf(System.currentTimeMillis() + System.nanoTime() % 1000000L));
                }
                List<PurchaseRecord> list = records.computeIfAbsent(r.getStockCode(), k -> new ArrayList<>());
                list.add(r);
            }
            saveRecords();
            oldPrefs.edit().clear().apply();
        } catch (Exception e) {}
    }

    private void saveRecords() {
        prefs.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
    }

    public List<PurchaseRecord> getRecords(String stockCode) {
        List<PurchaseRecord> list = records.get(stockCode);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public PurchaseRecord getFirstRecord(String stockCode) {
        List<PurchaseRecord> list = records.get(stockCode);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public boolean hasRecords(String stockCode) {
        List<PurchaseRecord> list = records.get(stockCode);
        return list != null && !list.isEmpty();
    }

    public double getAvgCost(String stockCode) {
        List<PurchaseRecord> list = records.get(stockCode);
        if (list == null || list.isEmpty()) return 0;
        double total = 0;
        for (PurchaseRecord r : list) total += r.getPrice();
        return total / list.size();
    }

    public Map<String, List<PurchaseRecord>> getAllRecords() {
        return new HashMap<>(records);
    }

    public PurchaseRecord addRecord(String stockCode, double price, String date) {
        List<PurchaseRecord> list = records.computeIfAbsent(stockCode, k -> new ArrayList<>());
        PurchaseRecord record = new PurchaseRecord(stockCode, price, date);
        list.add(record);
        saveRecords();
        return record;
    }

    public void deleteRecord(String stockCode, String id) {
        List<PurchaseRecord> list = records.get(stockCode);
        if (list != null) {
            list.removeIf(r -> r.getId().equals(id));
            if (list.isEmpty()) {
                records.remove(stockCode);
            }
            saveRecords();
        }
    }

    public void deleteAllRecords(String stockCode) {
        records.remove(stockCode);
        saveRecords();
    }
}
