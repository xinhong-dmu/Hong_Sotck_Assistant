package com.hong.xin.stock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.PurchaseRecord;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PurchaseRecordManager {

    private static final String PREF_NAME = "purchase_records";
    private static final String KEY_RECORDS = "records";

    private static PurchaseRecordManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, PurchaseRecord> records;

    private PurchaseRecordManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        records = loadRecords();
    }

    public static synchronized PurchaseRecordManager getInstance(Context context) {
        if (instance == null) {
            instance = new PurchaseRecordManager(context.getApplicationContext());
        }
        return instance;
    }

    private Map<String, PurchaseRecord> loadRecords() {
        try {
            String json = prefs.getString(KEY_RECORDS, "{}");
            Type type = new TypeToken<Map<String, PurchaseRecord>>() {}.getType();
            Map<String, PurchaseRecord> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void saveRecords() {
        prefs.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
    }

    public PurchaseRecord getRecord(String stockCode) {
        return records.get(stockCode);
    }

    public void saveRecord(String stockCode, double price, String date) {
        records.put(stockCode, new PurchaseRecord(stockCode, price, date));
        saveRecords();
    }

    public void deleteRecord(String stockCode) {
        records.remove(stockCode);
        saveRecords();
    }
}
