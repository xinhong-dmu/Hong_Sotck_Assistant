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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EastMoneyApi {

    private static final OkHttpClient CLIENT = HttpClientFactory.getClient();
    private static final Gson GSON = new Gson();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final StockDataCache CACHE = StockDataCache.getInstance();

    private static final String TAG = "EastMoneyApi";

    private static final ConcurrentHashMap<String, Object> IN_FLIGHT_LOCKS = new ConcurrentHashMap<>();
    private static final Object PLACEHOLDER = new Object();

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void fetchKline(String code, int days, Callback<List<KlineData>> callback) {
        List<KlineData> cached = CACHE.getKline(code, days);
        if (cached != null) {
            Log.d(TAG, "fetchKline cache hit: " + code + " days=" + days);
            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>(cached)));
            return;
        }

        String lockKey = "kline_" + code + "_" + days;
        if (IN_FLIGHT_LOCKS.putIfAbsent(lockKey, PLACEHOLDER) != null) {
            Log.d(TAG, "fetchKline dedup: " + code + " days=" + days);
            List<KlineData> stale = CACHE.getKlineStale(code, days);
            if (stale != null) {
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>(stale)));
            } else {
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            }
            return;
        }

        Log.d(TAG, "fetchKline tencent: " + code + " days=" + days);
        TencentApi.fetchKline(code, days, new TencentApi.Callback<List<KlineData>>() {
            @Override
            public void onResult(List<KlineData> tencentResult) {
                if (!tencentResult.isEmpty()) {
                    CACHE.putKline(code, days, new ArrayList<>(tencentResult));
                    Log.d(TAG, "fetchKline tencent success: " + code + " size=" + tencentResult.size());
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(tencentResult));
                } else {
                    Log.d(TAG, "fetchKline tencent empty, try eastmoney: " + code);
                    tryEastMoneyKlineFallback(code, days, lockKey, callback);
                }
            }
        });
    }

    public static double fetchLatestCloseSync(String code) {
        RealtimeQuote cached = CACHE.getQuote(code);
        if (cached != null && cached.getPrice() > 0) {
            return cached.getPrice();
        }

        double price = fetchLatestCloseFromSina(code);
        if (price <= 0) {
            price = TencentApi.fetchLatestCloseSync(code);
        }
        if (price <= 0) {
            price = fetchLatestCloseFromEastMoney(code);
        }
        return price;
    }

    private static double fetchLatestCloseFromEastMoney(String code) {
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
            Log.e(TAG, "fetchLatestCloseFromEastMoney error: " + e.getMessage());
        }
        return -1;
    }

    private static double fetchLatestCloseFromSina(String code) {
        try {
            String sinaCode = getSinaCode(code);
            String url = "https://hq.sinajs.cn/list=" + sinaCode;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://finance.sina.com.cn/")
                    .build();
            Response response = CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                String[] parts = body.split(",");
                if (parts.length > 3) {
                    return Double.parseDouble(parts[3]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchLatestCloseFromSina error: " + e.getMessage());
        }
        return -1;
    }

    public static void fetchRealtime(String code, Callback<RealtimeQuote> callback) {
        RealtimeQuote cached = CACHE.getQuote(code);
        if (cached != null && cached.getPrice() > 0) {
            Log.d(TAG, "fetchRealtime cache hit: " + code);
            MAIN_HANDLER.post(() -> callback.onResult(cached));
            return;
        }

        String lockKey = "quote_" + code;
        if (IN_FLIGHT_LOCKS.putIfAbsent(lockKey, PLACEHOLDER) != null) {
            Log.d(TAG, "fetchRealtime dedup: " + code);
            RealtimeQuote stale = CACHE.getQuoteStale(code);
            if (stale != null) {
                MAIN_HANDLER.post(() -> callback.onResult(stale));
            } else {
                MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
            }
            return;
        }

        fetchRealtimeFromSina(code, new Callback<RealtimeQuote>() {
            @Override
            public void onResult(RealtimeQuote sinaResult) {
                if (sinaResult.getPrice() > 0) {
                    CACHE.putQuote(code, sinaResult);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(sinaResult));
                } else {
                    Log.d(TAG, "fetchRealtime sina empty, try tencent: " + code);
                    TencentApi.fetchRealtime(code, new TencentApi.Callback<RealtimeQuote>() {
                        @Override
                        public void onResult(RealtimeQuote tencentResult) {
                            if (tencentResult.getPrice() > 0) {
                                CACHE.putQuote(code, tencentResult);
                                IN_FLIGHT_LOCKS.remove(lockKey);
                                MAIN_HANDLER.post(() -> callback.onResult(tencentResult));
                            } else {
                                Log.d(TAG, "fetchRealtime tencent empty, try eastmoney: " + code);
                                fetchRealtimeFromEastMoney(code, new Callback<RealtimeQuote>() {
                                    @Override
                                    public void onResult(RealtimeQuote emResult) {
                                        IN_FLIGHT_LOCKS.remove(lockKey);
                                        if (emResult.getPrice() > 0) {
                                            CACHE.putQuote(code, emResult);
                                            MAIN_HANDLER.post(() -> callback.onResult(emResult));
                                        } else {
                                            RealtimeQuote stale = CACHE.getQuoteStale(code);
                                            MAIN_HANDLER.post(() -> callback.onResult(stale != null ? stale : new RealtimeQuote()));
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private static void fetchRealtimeFromEastMoney(String code, Callback<RealtimeQuote> callback) {
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,f116,f162,f167,f168,f169,f170"
                + "&ut=7eea3edcaed734bea9c65a5e5297973b";

        Log.d(TAG, "fetchRealtime eastmoney: code=" + code + " secid=" + secid);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://quote.eastmoney.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchRealtime eastmoney onFailure: " + e.getMessage());
                callback.onResult(new RealtimeQuote());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchRealtime eastmoney response failed: code=" + response.code());
                        callback.onResult(new RealtimeQuote());
                        return;
                    }

                    String respBody = response.body().string();
                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        double rawPrice = getJsonDouble(data, "f43");
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
                        callback.onResult(quote);
                    } else {
                        Log.e(TAG, "fetchRealtime eastmoney: no data field");
                        callback.onResult(new RealtimeQuote());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchRealtime eastmoney parse error: " + e.getMessage(), e);
                    callback.onResult(new RealtimeQuote());
                }
            }
        });
    }

    private static void fetchRealtimeFromSina(String code, Callback<RealtimeQuote> callback) {
        try {
            String sinaCode = getSinaCode(code);
            String url = "https://hq.sinajs.cn/list=" + sinaCode;

            Log.d(TAG, "fetchRealtime sina fallback: code=" + code + " sinaCode=" + sinaCode);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://finance.sina.com.cn/")
                    .build();

            CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "fetchRealtime sina onFailure: " + e.getMessage());
                    callback.onResult(new RealtimeQuote());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "fetchRealtime sina response failed: code=" + response.code());
                            callback.onResult(new RealtimeQuote());
                            return;
                        }

                        String body = response.body().string();
                        Log.d(TAG, "fetchRealtime sina response: " + (body.length() > 200 ? body.substring(0, 200) : body));

                        RealtimeQuote quote = parseSinaResponse(code, body);
                        callback.onResult(quote);
                    } catch (Exception e) {
                        Log.e(TAG, "fetchRealtime sina parse error: " + e.getMessage(), e);
                        callback.onResult(new RealtimeQuote());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchRealtime sina build error: " + e.getMessage());
            callback.onResult(new RealtimeQuote());
        }
    }

    private static RealtimeQuote parseSinaResponse(String code, String body) {
        try {
            int eqIdx = body.indexOf('"');
            int endIdx = body.lastIndexOf('"');
            if (eqIdx < 0 || endIdx <= eqIdx) return new RealtimeQuote();

            String data = body.substring(eqIdx + 1, endIdx);
            String[] parts = data.split(",");
            if (parts.length < 32) return new RealtimeQuote();

            String name = parts[0];
            double open = parseDouble(parts[1]);
            double preClose = parseDouble(parts[2]);
            double price = parseDouble(parts[3]);
            double high = parseDouble(parts[4]);
            double low = parseDouble(parts[5]);
            double volume = parseDouble(parts[8]) / 100.0;
            double amount = parseDouble(parts[9]) / 10000.0;

            double change = price - preClose;
            double pctChg = preClose > 0 ? change / preClose * 100.0 : 0;

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
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "parseSinaResponse error: " + e.getMessage());
            return new RealtimeQuote();
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
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
                    Log.e(TAG, "searchSuggest onFailure: " + e.getMessage());
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
                        Log.e(TAG, "searchSuggest parse error: " + e.getMessage());
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "searchSuggest build error: " + e.getMessage());
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
        if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
            return "1." + code;
        } else {
            return "0." + code;
        }
    }

    private static String getSinaCode(String code) {
        if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
            return "sh" + code;
        } else {
            return "sz" + code;
        }
    }

    private static void tryEastMoneyKlineFallback(String code, int days, String lockKey, Callback<List<KlineData>> callback) {
        String secid = getSecid(code);
        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get"
                + "?fields1=f1,f2,f3,f4,f5,f6"
                + "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
                + "&ut=fa5fd1943c7b386f172d6893dbfd32bb"
                + "&klt=101&fqt=1"
                + "&secid=" + secid
                + "&end=20500101"
                + "&lmt=" + Math.max(days, 5);

        Log.d(TAG, "fetchKline eastmoney fallback url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://quote.eastmoney.com/")
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchKline eastmoney onFailure: " + e.getMessage());
                IN_FLIGHT_LOCKS.remove(lockKey);
                List<KlineData> stale = CACHE.getKlineStale(code, days);
                MAIN_HANDLER.post(() -> callback.onResult(stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchKline eastmoney response failed: code=" + response.code());
                        IN_FLIGHT_LOCKS.remove(lockKey);
                        List<KlineData> stale = CACHE.getKlineStale(code, days);
                        MAIN_HANDLER.post(() -> callback.onResult(stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
                        return;
                    }

                    String respBody = response.body().string();
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

                    if (!result.isEmpty()) {
                        CACHE.putKline(code, days, new ArrayList<>(result));
                    }
                    Log.d(TAG, "fetchKline eastmoney fallback: " + code + " days=" + days + " size=" + result.size());
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(result));
                } catch (Exception e) {
                    Log.e(TAG, "fetchKline eastmoney parse error: " + e.getMessage(), e);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    List<KlineData> stale = CACHE.getKlineStale(code, days);
                    MAIN_HANDLER.post(() -> callback.onResult(stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
                }
            }
        });
    }

    private static void tryTencentKlineFallback(String code, int days, String lockKey, Callback<List<KlineData>> callback) {
        TencentApi.fetchKline(code, days, new TencentApi.Callback<List<KlineData>>() {
            @Override
            public void onResult(List<KlineData> tencentResult) {
                if (!tencentResult.isEmpty()) {
                    CACHE.putKline(code, days, new ArrayList<>(tencentResult));
                }
                Log.d(TAG, "fetchKline tencent fallback: " + code + " days=" + days + " size=" + tencentResult.size());
                IN_FLIGHT_LOCKS.remove(lockKey);
                MAIN_HANDLER.post(() -> callback.onResult(tencentResult));
            }
        });
    }

    private static double getJsonDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0;
    }
}
