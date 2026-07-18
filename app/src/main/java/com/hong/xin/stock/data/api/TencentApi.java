package com.hong.xin.stock.data.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.RealtimeQuote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TencentApi {

    private static final OkHttpClient CLIENT = HttpClientFactory.getClient();
    private static final Gson GSON = new Gson();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final String TAG = "TencentApi";

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void fetchKline(String code, int days, Callback<List<KlineData>> callback) {
        String prefix = getTencentPrefix(code);
        String url = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get"
                + "?param=" + prefix + code + ",day,,," + Math.max(days, 5) + ",qfq";

        Log.d(TAG, "fetchKline url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://gu.qq.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchKline onFailure: " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchKline response failed: code=" + response.code());
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                        return;
                    }

                    String respBody = response.body().string();
                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    List<KlineData> result = new ArrayList<>();

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        String key = prefix + code;
                        if (data.has(key) && data.get(key).isJsonObject()) {
                            JsonObject stockData = data.getAsJsonObject(key);
                            JsonArray klines = null;
                            if (stockData.has("qfqday") && stockData.get("qfqday").isJsonArray()) {
                                klines = stockData.getAsJsonArray("qfqday");
                            } else if (stockData.has("day") && stockData.get("day").isJsonArray()) {
                                klines = stockData.getAsJsonArray("day");
                            }

                            if (klines != null) {
                                for (int i = 0; i < klines.size(); i++) {
                                    JsonArray item = klines.get(i).getAsJsonArray();
                                    if (item.size() >= 6) {
                                        try {
                                            String date = item.get(0).getAsString();
                                            double open = item.get(1).getAsDouble();
                                            double close = item.get(2).getAsDouble();
                                            double high = item.get(3).getAsDouble();
                                            double low = item.get(4).getAsDouble();
                                            double volume = item.get(5).getAsDouble();

                                            double preClose = i > 0 ? result.get(i - 1).getClose() : close;
                                            double change = close - preClose;
                                            double pctChg = preClose > 0 ? change / preClose * 100.0 : 0;
                                            double amplitude = low > 0 ? (high - low) / low * 100.0 : 0;

                                            result.add(new KlineData(date, open, close, high, low,
                                                    volume, 0, amplitude, pctChg, change, 0));
                                        } catch (Exception e) {
                                            Log.w(TAG, "fetchKline parse item error: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "fetchKline success: " + code + " days=" + days + " size=" + result.size());
                    MAIN_HANDLER.post(() -> callback.onResult(result));
                } catch (Exception e) {
                    Log.e(TAG, "fetchKline parse error: " + e.getMessage(), e);
                    MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                }
            }
        });
    }

    public static void fetchRealtime(String code, Callback<RealtimeQuote> callback) {
        String prefix = getTencentPrefix(code);
        String url = "http://qt.gtimg.cn/q=" + prefix + code;

        Log.d(TAG, "fetchRealtime url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://gu.qq.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchRealtime onFailure: " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchRealtime response failed: code=" + response.code());
                        MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
                        return;
                    }

                    String body = response.body().string();
                    RealtimeQuote quote = parseRealtimeResponse(code, body);
                    MAIN_HANDLER.post(() -> callback.onResult(quote));
                } catch (Exception e) {
                    Log.e(TAG, "fetchRealtime parse error: " + e.getMessage(), e);
                    MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
                }
            }
        });
    }

    public static double fetchLatestCloseSync(String code) {
        try {
            String prefix = getTencentPrefix(code);
            String url = "http://qt.gtimg.cn/q=" + prefix + code;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://gu.qq.com/")
                    .build();
            Response response = CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                RealtimeQuote quote = parseRealtimeResponse(code, body);
                if (quote.getPrice() > 0) {
                    return quote.getPrice();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchLatestCloseSync error: " + e.getMessage());
        }
        return -1;
    }

    private static RealtimeQuote parseRealtimeResponse(String code, String body) {
        try {
            int eqIdx = body.indexOf('"');
            int endIdx = body.lastIndexOf('"');
            if (eqIdx < 0 || endIdx <= eqIdx) return new RealtimeQuote();

            String data = body.substring(eqIdx + 1, endIdx);
            String[] parts = data.split("~");
            if (parts.length < 40) return new RealtimeQuote();

            String name = parts[1];
            double price = parseDouble(parts[3]);
            double preClose = parseDouble(parts[4]);
            double open = parseDouble(parts[5]);
            double volume = parseDouble(parts[6]);
            double high = parseDouble(parts[33]);
            double low = parseDouble(parts[34]);
            double amount = parseDouble(parts[37]);
            double pctChg = parseDouble(parts[32]);
            double change = price - preClose;
            double pe = parseDouble(parts[39]);

            return new RealtimeQuote.Builder()
                    .name(name)
                    .code(code)
                    .price(price)
                    .high(high)
                    .low(low)
                    .open(open)
                    .volume(volume)
                    .amount(amount)
                    .pctChg(pctChg)
                    .change(change)
                    .preClose(preClose)
                    .pe(pe)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "parseRealtimeResponse error: " + e.getMessage());
            return new RealtimeQuote();
        }
    }

    private static String getTencentPrefix(String code) {
        if (code.equals("000001") || code.equals("000016") || code.equals("000300")
                || code.equals("000688") || code.equals("000852")) {
            return "sh";
        }
        if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
            return "sh";
        } else if (code.startsWith("8") || code.startsWith("4")) {
            return "bj";
        } else {
            return "sz";
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
