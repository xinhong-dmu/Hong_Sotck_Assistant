package com.hong.xin.stock.data.repository;

import com.hong.xin.stock.data.model.TradeRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TradeRepository {

    private static final String RECORDS_FILE = "trade_records.csv";
    private static final String LOG_FILE = "trade_log.txt";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String readLog(File cacheDir) {
        File logFile = new File(cacheDir, LOG_FILE);
        if (!logFile.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException ignored) {
        }
        return sb.toString();
    }

    public static void appendLog(File cacheDir, String message) {
        File logFile = new File(cacheDir, LOG_FILE);
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(String.format("[%s] %s%n", DATE_FMT.format(new Date()), message));
        } catch (IOException ignored) {
        }
    }

    public static void saveRecord(File cacheDir, String stockName, String stockCode,
                                  double buyPrice, double highestPrice, double exitPrice,
                                  double profitPct, String reason) {
        File recordsFile = new File(cacheDir, RECORDS_FILE);
        boolean exists = recordsFile.exists();
        try (FileWriter fw = new FileWriter(recordsFile, true)) {
            if (!exists) {
                fw.write("time,stockName,stockCode,buyPrice,highestPrice,exitPrice,profitPct,reason\n");
            }
            String time = DATE_FMT.format(new Date());
            fw.write(String.format(Locale.US, "%s,%s,%s,%.4f,%.4f,%.4f,%.2f,%s\n",
                    time, stockName, stockCode, buyPrice, highestPrice, exitPrice, profitPct, reason));
        } catch (IOException ignored) {
        }
    }

    public static List<TradeRecord> readRecords(File cacheDir) {
        List<TradeRecord> records = new ArrayList<>();
        File recordsFile = new File(cacheDir, RECORDS_FILE);
        if (!recordsFile.exists()) return records;

        try (BufferedReader br = new BufferedReader(new FileReader(recordsFile))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 8) {
                    try {
                        TradeRecord record = new TradeRecord(
                                parts[0], parts[1], parts[2],
                                Double.parseDouble(parts[3]),
                                Double.parseDouble(parts[4]),
                                Double.parseDouble(parts[5]),
                                Double.parseDouble(parts[6]),
                                parts[7]
                        );
                        records.add(record);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return records;
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());
        return fields.toArray(new String[0]);
    }
}
