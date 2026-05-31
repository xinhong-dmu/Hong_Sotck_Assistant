package com.hong.xin.stock.data.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Stock;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EastMoneyApi {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void fetchKline(String code, int days, Callback<List<KlineData>> callback) {
        String secid = getSecid(code);
        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get"
                + "?fields1=f1,f2,f3,f4,f5,f6"
                + "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
                + "&ut=fa5fd1943c7b386f172d6893dbfd32bb"
                + "&klt=101&fqt=1"
                + "&secid=" + secid
                + "&end=20500101"
                + "&lmt=" + Math.max(days, 5);

        Log.d("EastMoneyApi", "fetchKline url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://quote.eastmoney.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EastMoneyApi", "fetchKline onFailure: " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e("EastMoneyApi", "fetchKline response failed: code=" + response.code());
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                        return;
                    }

                    String respBody = response.body().string();
                    Log.d("EastMoneyApi", "fetchKline response: " + (respBody.length() > 300 ? respBody.substring(0, 300) : respBody));
                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    List<KlineData> result = new ArrayList<>();

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("klines") && data.get("klines").isJsonArray()) {
                            JsonArray klines = data.getAsJsonArray("klines");

                            for (int i = 0; i < klines.size(); i++) {
                                String line = klines.get(i).getAsString();
                                String[] parts = line.split(",");
                                if (parts.length >= 11) {
                                    try {
                                        String date = parts[0].substring(0, 10);
                                        double open = Double.parseDouble(parts[1]);
                                        double close = Double.parseDouble(parts[2]);
                                        double high = Double.parseDouble(parts[3]);
                                        double low = Double.parseDouble(parts[4]);
                                        double volume = Double.parseDouble(parts[5]) / 100.0;
                                        double amount = Double.parseDouble(parts[6]) / 10000.0;
                                        double amplitude = Double.parseDouble(parts[7]);
                                        double pctChg = Double.parseDouble(parts[8]);
                                        double change = Double.parseDouble(parts[9]);
                                        double turnover = Double.parseDouble(parts[10]);

                                        result.add(new KlineData(date, open, close, high, low,
                                                volume, amount, amplitude, pctChg, change, turnover));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                    MAIN_HANDLER.post(() -> callback.onResult(result));
                } catch (Exception e) {
                    Log.e("EastMoneyApi", "fetchKline parse error: " + e.getMessage(), e);
                    MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                }
            }
        });
    }

    // 同步获取K线数据，返回最新收盘价；失败返回-1
    public static double fetchLatestCloseSync(String code) {
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,f116,f162,f167,f168,f169,f170"
                + "&ut=fa5fd1943c7b386f172d6893dbfd32bb";
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://quote.eastmoney.com/")
                    .build();
            Response response = CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String respBody = response.body().string();
                JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                if (json.has("data") && json.get("data").isJsonObject()) {
                    JsonObject data = json.getAsJsonObject("data");
                    double rawPrice = getJsonDouble(data, "f43");
                    if (rawPrice > 0) return rawPrice / 100.0;
                }
            }
        } catch (Exception e) {
            Log.e("EastMoneyApi", "fetchLatestCloseSync error: " + e.getMessage());
        }
        return -1;
    }

    public static void fetchRealtime(String code, Callback<RealtimeQuote> callback) {
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,f116,f162,f167,f168,f169,f170"
                + "&ut=7eea3edcaed734bea9c65a5e5297973b";

        Log.d("EastMoneyApi", "fetchRealtime: code=" + code + " secid=" + secid);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://quote.eastmoney.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EastMoneyApi", "fetchRealtime onFailure: " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e("EastMoneyApi", "fetchRealtime response failed: code=" + response.code());
                        MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
                        return;
                    }

                    String respBody = response.body().string();
                    Log.d("EastMoneyApi", "fetchRealtime response: " + (respBody.length() > 200 ? respBody.substring(0, 200) : respBody));
                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        double rawPrice = getJsonDouble(data, "f43");
                        Log.d("EastMoneyApi", "fetchRealtime rawPrice(f43)=" + rawPrice + " price=" + (rawPrice / 100.0));
                        RealtimeQuote quote = new RealtimeQuote.Builder()
                                .name(data.has("f58") && !data.get("f58").isJsonNull() ? data.get("f58").getAsString() : "")
                                .code(code)
                                .price(rawPrice / 100.0)
                                .high(getJsonDouble(data, "f44") / 100.0)
                                .low(getJsonDouble(data, "f45") / 100.0)
                                .open(getJsonDouble(data, "f46") / 100.0)
                                .volume(getJsonDouble(data, "f47"))
                                .amount(getJsonDouble(data, "f48"))
                                .pctChg(getJsonDouble(data, "f170"))
                                .change(getJsonDouble(data, "f169") / 100.0)
                                .preClose(getJsonDouble(data, "f60") / 100.0)
                                .pe(getJsonDouble(data, "f162"))
                                .build();
                        MAIN_HANDLER.post(() -> callback.onResult(quote));
                    } else {
                        Log.e("EastMoneyApi", "fetchRealtime: no data field in response");
                        MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
                    }
                } catch (Exception e) {
                    Log.e("EastMoneyApi", "fetchRealtime parse error: " + e.getMessage(), e);
                    MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
                }
            }
        });
    }

    public static void searchSuggest(String keyword, Callback<List<Stock>> callback) {
        try {
            String url = "https://searchadapter.eastmoney.com/api/suggest/get"
                    + "?input=" + java.net.URLEncoder.encode(keyword, "UTF-8")
                    + "&count=10&type=14";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://so.eastmoney.com/")
                    .build();

            CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                            return;
                        }

                        String respBody = response.body().string();
                        JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                        List<Stock> stocks = new ArrayList<>();

                        if (json.has("data") && json.get("data").isJsonArray()) {
                            JsonArray arr = json.getAsJsonArray("data");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonObject item = arr.get(i).getAsJsonObject();
                                String code = item.has("Code") ? item.get("Code").getAsString() : "";
                                String name = item.has("Name") ? item.get("Name").getAsString() : "";
                                if (!code.isEmpty() && !name.isEmpty()) {
                                    stocks.add(new Stock(code, name));
                                }
                            }
                        }
                        MAIN_HANDLER.post(() -> callback.onResult(stocks));
                    } catch (Exception e) {
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                    }
                }
            });
        } catch (Exception e) {
            callback.onResult(new ArrayList<>());
        }
    }

    public static void fetchStockList(Callback<List<Stock>> callback) {
        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
    }

    public static void fetchEtfList(Callback<List<Stock>> callback) {
        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
    }

    private static String getSecid(String code) {
        if (code.startsWith("6") || code.startsWith("9")) {
            return "1." + code;
        } else {
            return "0." + code;
        }
    }

    private static double getJsonDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0;
    }
}
