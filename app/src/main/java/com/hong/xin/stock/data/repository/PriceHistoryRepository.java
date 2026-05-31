package com.hong.xin.stock.data.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.PriceRecord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PriceHistoryRepository {

    private static final String FILE_NAME = "price_history.json";
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<List<PriceRecord>>() {}.getType();

    public static List<PriceRecord> readAll(File cacheDir) {
        File file = new File(cacheDir, FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            List<PriceRecord> records = GSON.fromJson(reader, TYPE);
            return records != null ? records : new ArrayList<>();
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    public static void appendRecord(File cacheDir, PriceRecord record) {
        List<PriceRecord> records = readAll(cacheDir);
        records.add(0, record);
        writeAll(cacheDir, records);
    }

    public static void deleteRecord(File cacheDir, PriceRecord target) {
        List<PriceRecord> records = readAll(cacheDir);
        records.removeIf(r -> r.getTime().equals(target.getTime())
                && r.getStockCode().equals(target.getStockCode()));
        writeAll(cacheDir, records);
    }

    private static void writeAll(File cacheDir, List<PriceRecord> records) {
        File file = new File(cacheDir, FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(records, writer);
        } catch (IOException ignored) {
        }
    }
}
