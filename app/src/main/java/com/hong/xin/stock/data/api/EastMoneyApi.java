package com.hong.xin.stock.data.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hong.xin.stock.data.StockListCache;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Stock;

import java.io.IOException;
import java.net.URLEncoder;
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
    private static final ConcurrentHashMap<String, RealtimeQuote> LAST_ETF_EXTRA = new ConcurrentHashMap<>();
 
    private static final String TAG = "EastMoneyApi";

    private static final ConcurrentHashMap<String, Object> IN_FLIGHT_LOCKS = new ConcurrentHashMap<>();
    private static final Object PLACEHOLDER = new Object();

    private static StockListCache stockListCache;
    private static boolean initializing = false;

    public static void init(StockListCache cache) {
        stockListCache = cache;
    }

    public static synchronized void ensureInit() {
        if (initializing) return;
        initializing = true;
        try {
            if (stockListCache != null) {
                List<Stock> cached = stockListCache.load();
                if (cached != null && !cached.isEmpty()) {
                    allStocksCache = cached;
                    loadFailCount = 0;
                    Log.i(TAG, "ensureInit loaded from local cache: size=" + cached.size());
                } else {
                    refreshStockLists();
                }
            }
        } finally {
            initializing = false;
        }
    }

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String REFERER_EM = "https://quote.eastmoney.com/";
    private static final String REFERER_SINA = "https://finance.sina.com.cn/";
    private static final String REFERER_SO = "https://so.eastmoney.com/";
    private static final String UT = "fa5fd1943c7b386f172d6893dbfd32bb";

    public interface Callback<T> {
        void onResult(T result);
    }

    // ──────────────────────────────────────────────
    // 补充指标 (PE/PB/总市值/换手率/量比等) - 直接从EastMoney获取
    // ──────────────────────────────────────────────

    public static void fetchExtra(String code, Callback<RealtimeQuote> callback) {
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,"
                + "f116,f117,f162,f167,f168,f169,f170,f288,f289"
                + "&ut=" + UT;

        Log.d(TAG, "fetchExtra url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_EM)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchExtra onFailure: " + e.getMessage(), e);
                RealtimeQuote cached = LAST_ETF_EXTRA.get(code);
                MAIN_HANDLER.post(() -> callback.onResult(cached != null ? cached : new RealtimeQuote()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        deliverExtraCachedOrEmpty(code, callback);
                        return;
                    }
                    String respBody = response.body().string();
                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject d = json.getAsJsonObject("data");
                        double div = getPriceDiv(code);
                        double iopv = getJsonDouble(d, "f288") / div;
                        RealtimeQuote quote = new RealtimeQuote.Builder()
                                .name(getJsonStr(d, "f58"))
                                .code(code)
                                .price(getJsonDouble(d, "f43") / div)
                                .high(getJsonDouble(d, "f44") / div)
                                .low(getJsonDouble(d, "f45") / div)
                                .open(getJsonDouble(d, "f46") / div)
                                .volume(getJsonDouble(d, "f47") / 100.0)
                                .amount(getJsonDouble(d, "f48") / 10000.0)
                                .pctChg(getJsonDouble(d, "f170") / 100.0)
                                .change(getJsonDouble(d, "f169") / div)
                                .preClose(getJsonDouble(d, "f60") / div)
                                .pe(getJsonDouble(d, "f162") / 100.0)
                                .pb(getJsonDouble(d, "f167") / 100.0)
                                .turnoverRate(getJsonDouble(d, "f168") / 100.0)
                                .volumeRatio(getJsonDouble(d, "f50") / 100.0)
                                .totalMarketCap(getJsonDouble(d, "f116"))
                                .circulatingMarketCap(getJsonDouble(d, "f117"))
                                .limitUp(getJsonDouble(d, "f51") / div)
                                .limitDown(getJsonDouble(d, "f52") / div)
                                .iopv(iopv)
                                .premiumRate(getJsonDouble(d, "f289"))
                                .build();

                        if (quote.getIopv() > 0) {
                            LAST_ETF_EXTRA.put(code, quote);
                        }

                        if (iopv <= 0) {
                            Log.d(TAG, "fetchExtra EastMoney IOPV invalid, try Tencent: " + code);
                            TencentApi.fetchEtfIopv(code, (tencentIopv, tencentPremium) -> {
                                RealtimeQuote result;
                                if (tencentIopv > 0) {
                                    result = new RealtimeQuote.Builder(quote)
                                            .iopv(tencentIopv)
                                            .premiumRate(tencentPremium)
                                            .build();
                                    LAST_ETF_EXTRA.put(code, result);
                                } else {
                                    result = LAST_ETF_EXTRA.containsKey(code) ? LAST_ETF_EXTRA.get(code) : quote;
                                }
                                Log.d(TAG, "fetchExtra OK (Tencent fallback): " + code
                                        + " pe=" + result.getPe() + " pb=" + result.getPb()
                                        + " iopv=" + result.getIopv() + " premiumRate=" + result.getPremiumRate());
                                MAIN_HANDLER.post(() -> callback.onResult(result));
                            });
                            return;
                        }

                        RealtimeQuote result = (quote.getIopv() > 0 || !LAST_ETF_EXTRA.containsKey(code))
                                ? quote : LAST_ETF_EXTRA.get(code);
                        Log.d(TAG, "fetchExtra OK: " + code + " pe=" + result.getPe() + " pb=" + result.getPb()
                                    + " iopv=" + result.getIopv() + " premiumRate=" + result.getPremiumRate()
                                    + (result != quote ? " (cached)" : ""));
                        MAIN_HANDLER.post(() -> callback.onResult(result));
                    } else {
                        deliverExtraCachedOrEmpty(code, callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchExtra parse error: " + e.getMessage(), e);
                    deliverExtraCachedOrEmpty(code, callback);
                }
            }
        });
    }

    private static void deliverExtraCachedOrEmpty(String code, Callback<RealtimeQuote> callback) {
        RealtimeQuote cached = LAST_ETF_EXTRA.get(code);
        if (cached != null) {
            Log.d(TAG, "fetchExtra no data, use cached: " + code);
            MAIN_HANDLER.post(() -> callback.onResult(cached));
        } else {
            MAIN_HANDLER.post(() -> callback.onResult(new RealtimeQuote()));
        }
    }

    private static double getPriceDiv(String code) {
        if (code.startsWith("0") || code.startsWith("3") || code.startsWith("6")) {
            return 100.0;
        }
        return 1000.0;
    }

    // ──────────────────────────────────────────────
    // 实时行情 (Real-time Quote) - 含完整字段
    // ──────────────────────────────────────────────

    public static void fetchRealtime(String code, Callback<RealtimeQuote> callback) {
        RealtimeQuote cached = CACHE.getQuote(code);
        if (cached != null && cached.getPrice() > 0) {
            Log.d(TAG, "fetchRealtime cache hit: code=" + code);
            MAIN_HANDLER.post(() -> callback.onResult(cached));
            return;
        }

        String lockKey = "quote_" + code;
        if (IN_FLIGHT_LOCKS.putIfAbsent(lockKey, PLACEHOLDER) != null) {
            Log.d(TAG, "fetchRealtime dedup, returning stale: code=" + code);
            RealtimeQuote stale = CACHE.getQuoteStale(code);
            MAIN_HANDLER.post(() -> callback.onResult(stale != null ? stale : new RealtimeQuote()));
            return;
        }

        Log.d(TAG, "fetchRealtime start: code=" + code);
        fetchRealtimeFromSina(code, new Callback<RealtimeQuote>() {
            @Override
            public void onResult(RealtimeQuote sinaResult) {
                if (sinaResult.getPrice() > 0) {
                    CACHE.putQuote(code, sinaResult);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(sinaResult));
                } else {
                    Log.d(TAG, "fetchRealtime Sina empty, try Tencent: code=" + code);
                    TencentApi.fetchRealtime(code, new TencentApi.Callback<RealtimeQuote>() {
                        @Override
                        public void onResult(RealtimeQuote tencentResult) {
                            if (tencentResult.getPrice() > 0) {
                                CACHE.putQuote(code, tencentResult);
                                IN_FLIGHT_LOCKS.remove(lockKey);
                                MAIN_HANDLER.post(() -> callback.onResult(tencentResult));
                            } else {
                                Log.d(TAG, "fetchRealtime Tencent empty, try EastMoney: code=" + code);
                                fetchRealtimeFromEastMoney(code, new Callback<RealtimeQuote>() {
                                    @Override
                                    public void onResult(RealtimeQuote emResult) {
                                        IN_FLIGHT_LOCKS.remove(lockKey);
                                        if (emResult.getPrice() > 0) {
                                            CACHE.putQuote(code, emResult);
                                            MAIN_HANDLER.post(() -> callback.onResult(emResult));
                                        } else {
                                            RealtimeQuote stale = CACHE.getQuoteStale(code);
                                            MAIN_HANDLER.post(() -> callback.onResult(
                                                    stale != null ? stale : new RealtimeQuote()));
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
        double div = getPriceDiv(code);
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,"
                + "f116,f117,f162,f163,f167,f168,f169,f170,f171,f172,f173,f174,f175,"
                + "f184,f185,f188,f189,f228,f229,f288,f289"
                + "&ut=" + UT;

        Log.d(TAG, "fetchRealtimeFromEastMoney url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_EM)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchRealtimeFromEastMoney onFailure: " + e.getMessage(), e);
                callback.onResult(new RealtimeQuote());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchRealtimeFromEastMoney response code=" + response.code());
                        callback.onResult(new RealtimeQuote());
                        return;
                    }
                    String respBody = response.body().string();
                    Log.d(TAG, "fetchRealtimeFromEastMoney raw: " + truncate(respBody, 300));

                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject d = json.getAsJsonObject("data");
                        RealtimeQuote quote = new RealtimeQuote.Builder()
                                .name(getJsonStr(d, "f58"))
                                .code(code)
                                .price(getJsonDouble(d, "f43") / div)
                                .high(getJsonDouble(d, "f44") / div)
                                .low(getJsonDouble(d, "f45") / div)
                                .open(getJsonDouble(d, "f46") / div)
                                .volume(getJsonDouble(d, "f47"))
                                .amount(getJsonDouble(d, "f48"))
                                .pctChg(getJsonDouble(d, "f170"))
                                .change(getJsonDouble(d, "f169") / div)
                                .preClose(getJsonDouble(d, "f60") / div)
                                .pe(getJsonDouble(d, "f162"))
                                .peTTM(getJsonDouble(d, "f163"))
                                .pb(getJsonDouble(d, "f167"))
                                .turnoverRate(getJsonDouble(d, "f168"))
                                .volumeRatio(getJsonDouble(d, "f50") / 100.0)
                                .totalMarketCap(getJsonDouble(d, "f116"))
                                .circulatingMarketCap(getJsonDouble(d, "f117"))
                                .limitUp(getJsonDouble(d, "f51") / div)
                                .limitDown(getJsonDouble(d, "f52") / div)
                                .eps(getJsonDouble(d, "f228"))
                                .dividendYield(getJsonDouble(d, "f188"))
                                .ma5(getJsonDouble(d, "f172"))
                                .ma10(getJsonDouble(d, "f173"))
                                .ma20(getJsonDouble(d, "f174"))
                                .ma30(getJsonDouble(d, "f175"))
                                .ma60(getJsonDouble(d, "f171"))
                                .iopv(getJsonDouble(d, "f288") / div)
                                .premiumRate(getJsonDouble(d, "f289"))
                                .build();
                        Log.i(TAG, "fetchRealtimeFromEastMoney OK: " + code
                                + " price=" + quote.getPrice()
                                + " pctChg=" + quote.getPctChg()
                                + " volRatio=" + quote.getVolumeRatio()
                                + " turnover=" + quote.getTurnoverRate());
                        callback.onResult(quote);
                    } else {
                        Log.w(TAG, "fetchRealtimeFromEastMoney no data for: " + code);
                        callback.onResult(new RealtimeQuote());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchRealtimeFromEastMoney parse error: " + e.getMessage(), e);
                    callback.onResult(new RealtimeQuote());
                }
            }
        });
    }

    private static void fetchRealtimeFromSina(String code, Callback<RealtimeQuote> callback) {
        try {
            String sinaCode = getSinaCode(code);
            String url = "https://hq.sinajs.cn/list=" + sinaCode;
            Log.d(TAG, "fetchRealtimeFromSina url: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", UA)
                    .addHeader("Referer", REFERER_SINA)
                    .build();

            CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "fetchRealtimeFromSina onFailure: " + e.getMessage(), e);
                    callback.onResult(new RealtimeQuote());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "fetchRealtimeFromSina response code=" + response.code());
                            callback.onResult(new RealtimeQuote());
                            return;
                        }
                        String body = response.body().string();
                        Log.d(TAG, "fetchRealtimeFromSina raw: " + truncate(body, 200));
                        callback.onResult(parseSinaResponse(code, body));
                    } catch (Exception e) {
                        Log.e(TAG, "fetchRealtimeFromSina parse error: " + e.getMessage(), e);
                        callback.onResult(new RealtimeQuote());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchRealtimeFromSina error: " + e.getMessage(), e);
            callback.onResult(new RealtimeQuote());
        }
    }

    // ──────────────────────────────────────────────
    // 最新收盘价同步获取
    // ──────────────────────────────────────────────

    public static double fetchLatestCloseSync(String code) {
        RealtimeQuote cached = CACHE.getQuote(code);
        if (cached != null && cached.getPrice() > 0) {
            Log.d(TAG, "fetchLatestCloseSync cache hit: " + code + " price=" + cached.getPrice());
            return cached.getPrice();
        }
        double price = fetchLatestCloseFromSina(code);
        if (price <= 0) {
            price = TencentApi.fetchLatestCloseSync(code);
        }
        if (price <= 0) {
            price = fetchLatestCloseFromEastMoney(code);
        }
        Log.d(TAG, "fetchLatestCloseSync result: " + code + " price=" + price);
        return price;
    }

    private static double fetchLatestCloseFromEastMoney(String code) {
        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/get"
                + "?secid=" + secid
                + "&fields=f43,f60"
                + "&ut=" + UT;
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", UA)
                    .addHeader("Referer", REFERER_EM)
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
                    .addHeader("User-Agent", UA)
                    .addHeader("Referer", REFERER_SINA)
                    .build();
            Response response = CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                String[] parts = body.split(",");
                if (parts.length > 3) {
                    return parseDouble(parts[3]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchLatestCloseFromSina error: " + e.getMessage());
        }
        return -1;
    }

    // ──────────────────────────────────────────────
    // K线数据 (K-line) - 支持多周期
    // klt: 1=1分钟, 5=5分钟, 15=15分钟, 30=30分钟,
    //      60=60分钟, 101=日, 102=周, 103=月
    // fqt: 0=不复权, 1=前复权, 2=后复权
    // ──────────────────────────────────────────────

    public static void fetchKline(String code, int days, Callback<List<KlineData>> callback) {
        fetchKlineWithPeriod(code, days, 101, 1, callback);
    }

    public static void fetchKlineWithPeriod(String code, int count, int klt, int fqt,
                                            Callback<List<KlineData>> callback) {
        String cacheKey = "kline_" + code + "_" + klt + "_" + fqt + "_" + count;
        List<KlineData> cached = CACHE.getKline(code, cacheKey);
        if (cached != null) {
            Log.d(TAG, "fetchKline cache hit: " + cacheKey + " size=" + cached.size());
            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>(cached)));
            return;
        }

        String lockKey = "kline_" + cacheKey;
        if (IN_FLIGHT_LOCKS.putIfAbsent(lockKey, PLACEHOLDER) != null) {
            Log.d(TAG, "fetchKline dedup: " + cacheKey);
            List<KlineData> stale = CACHE.getKlineStale(code, cacheKey);
            MAIN_HANDLER.post(() -> callback.onResult(
                    stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
            return;
        }

        Log.d(TAG, "fetchKline start: code=" + code + " count=" + count
                + " klt=" + klt + " fqt=" + fqt);

        if (klt == 101) {
            TencentApi.fetchKline(code, count, new TencentApi.Callback<List<KlineData>>() {
                @Override
                public void onResult(List<KlineData> tencentResult) {
                    if (!tencentResult.isEmpty()) {
                        CACHE.putKline(code, cacheKey, new ArrayList<>(tencentResult));
                        IN_FLIGHT_LOCKS.remove(lockKey);
                        Log.d(TAG, "fetchKline Tencent OK: " + cacheKey + " size=" + tencentResult.size());
                        MAIN_HANDLER.post(() -> callback.onResult(tencentResult));
                    } else {
                        Log.d(TAG, "fetchKline Tencent empty, try EastMoney: " + code);
                        tryEastMoneyKlineFallback(code, count, klt, fqt, lockKey, cacheKey, callback);
                    }
                }
            });
        } else {
            tryEastMoneyKlineFallback(code, count, klt, fqt, lockKey, cacheKey, callback);
        }
    }

    private static void tryEastMoneyKlineFallback(String code, int count, int klt, int fqt,
                                                   String lockKey, String cacheKey,
                                                   Callback<List<KlineData>> callback) {
        String secid = getSecid(code);
        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get"
                + "?fields1=f1,f2,f3,f4,f5,f6"
                + "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
                + "&ut=" + UT
                + "&klt=" + klt
                + "&fqt=" + fqt
                + "&secid=" + secid
                + "&end=20500101"
                + "&lmt=" + Math.max(count, 5);

        Log.d(TAG, "fetchKline EastMoney url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_EM)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchKline EastMoney onFailure: " + e.getMessage(), e);
                IN_FLIGHT_LOCKS.remove(lockKey);
                List<KlineData> stale = CACHE.getKlineStale(code, cacheKey);
                MAIN_HANDLER.post(() -> callback.onResult(
                        stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchKline EastMoney response code=" + response.code());
                        IN_FLIGHT_LOCKS.remove(lockKey);
                        List<KlineData> stale = CACHE.getKlineStale(code, cacheKey);
                        MAIN_HANDLER.post(() -> callback.onResult(
                                stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
                        return;
                    }

                    String respBody = response.body().string();
                    Log.d(TAG, "fetchKline EastMoney raw: " + truncate(respBody, 200));

                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    List<KlineData> result = new ArrayList<>();

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("klines") && data.get("klines").isJsonArray()) {
                            JsonArray klines = data.getAsJsonArray("klines");
                            Log.d(TAG, "fetchKline EastMoney klines count=" + klines.size());

                            for (int i = 0; i < klines.size(); i++) {
                                String line = klines.get(i).getAsString();
                                String[] parts = line.split(",");
                                if (parts.length >= 11) {
                                    try {
                                        String date = parts[0];
                                        if (date.length() > 10) date = date.substring(0, 10);
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
                                    } catch (NumberFormatException e) {
                                        Log.w(TAG, "fetchKline parse item error: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    if (!result.isEmpty()) {
                        CACHE.putKline(code, cacheKey, new ArrayList<>(result));
                    }
                    Log.i(TAG, "fetchKline EastMoney OK: code=" + code + " size=" + result.size()
                            + " klt=" + klt + " fqt=" + fqt);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(result));
                } catch (Exception e) {
                    Log.e(TAG, "fetchKline EastMoney parse error: " + e.getMessage(), e);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    List<KlineData> stale = CACHE.getKlineStale(code, cacheKey);
                    MAIN_HANDLER.post(() -> callback.onResult(
                            stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    // 分时数据 (Minute/Time-sharing Line)
    // ──────────────────────────────────────────────

    public static void fetchMinuteLine(String code, Callback<List<MinuteLineData>> callback) {
        fetchMinuteLineDays(code, 1, callback);
    }

    public static void fetchMinuteLineDays(String code, int ndays,
                                           Callback<List<MinuteLineData>> callback) {
        String cacheKey = "minute_" + code + "_" + ndays;
        List<MinuteLineData> cached = CACHE.getMinuteLine(code, cacheKey);
        if (cached != null) {
            Log.d(TAG, "fetchMinuteLine cache hit: " + cacheKey + " size=" + cached.size());
            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>(cached)));
            return;
        }

        String lockKey = "minute_" + cacheKey;
        if (IN_FLIGHT_LOCKS.putIfAbsent(lockKey, PLACEHOLDER) != null) {
            Log.d(TAG, "fetchMinuteLine dedup: " + cacheKey);
            List<MinuteLineData> stale = CACHE.getMinuteLineStale(code, cacheKey);
            MAIN_HANDLER.post(() -> callback.onResult(
                    stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
            return;
        }

        String secid = getSecid(code);
        String url = "https://push2.eastmoney.com/api/qt/stock/trends2/get"
                + "?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13"
                + "&fields2=f51,f52,f53,f54,f55,f56,f57,f58"
                + "&ut=" + UT
                + "&ndays=" + ndays
                + "&secid=" + secid;

        Log.d(TAG, "fetchMinuteLine url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_EM)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchMinuteLine onFailure: " + e.getMessage(), e);
                IN_FLIGHT_LOCKS.remove(lockKey);
                List<MinuteLineData> stale = CACHE.getMinuteLineStale(code, cacheKey);
                MAIN_HANDLER.post(() -> callback.onResult(
                        stale != null ? new ArrayList<>(stale) : new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchMinuteLine response code=" + response.code());
                        IN_FLIGHT_LOCKS.remove(lockKey);
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                        return;
                    }

                    String respBody = response.body().string();
                    Log.d(TAG, "fetchMinuteLine raw: " + truncate(respBody, 300));

                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    List<MinuteLineData> result = new ArrayList<>();

                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        double preClose = getJsonDouble(data, "preClose");
                        if (preClose <= 0) preClose = getJsonDouble(data, "f60");

                        Log.d(TAG, "fetchMinuteLine preClose=" + preClose);

                        if (data.has("trends") && data.get("trends").isJsonArray()) {
                            JsonArray trends = data.getAsJsonArray("trends");
                            Log.d(TAG, "fetchMinuteLine trends count=" + trends.size());

                            for (int i = 0; i < trends.size(); i++) {
                                String line = trends.get(i).getAsString();
                                String[] parts = line.split(",");
                                if (parts.length >= 8) {
                                    try {
                                        String time = parts[0];
                                        double price = Double.parseDouble(parts[1]);
                                        double avgPrice = Double.parseDouble(parts[2]);
                                        double volume = Double.parseDouble(parts[3]);
                                        double amount = Double.parseDouble(parts[4]) / 10000.0;

                                        result.add(new MinuteLineData(time, price, avgPrice,
                                                volume, amount, preClose));
                                    } catch (NumberFormatException e) {
                                        Log.w(TAG, "fetchMinuteLine parse item error: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    if (!result.isEmpty()) {
                        CACHE.putMinuteLine(code, cacheKey, new ArrayList<>(result));
                    }
                    Log.i(TAG, "fetchMinuteLine OK: code=" + code + " ndays=" + ndays
                            + " size=" + result.size());
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(result));
                } catch (Exception e) {
                    Log.e(TAG, "fetchMinuteLine parse error: " + e.getMessage(), e);
                    IN_FLIGHT_LOCKS.remove(lockKey);
                    MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    // 移动平均线 (MA) 计算 - 基于K线数据
    // ──────────────────────────────────────────────

    public static void calculateMA(List<KlineData> klines, int period, Callback<double[]> callback) {
        if (klines == null || klines.isEmpty()) {
            Log.w(TAG, "calculateMA empty input, period=" + period);
            MAIN_HANDLER.post(() -> callback.onResult(new double[0]));
            return;
        }
        int size = klines.size();
        double[] ma = new double[size];
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += klines.get(i).getClose();
            if (i >= period - 1) {
                if (i >= period) sum -= klines.get(i - period).getClose();
                ma[i] = sum / period;
            } else {
                ma[i] = 0;
            }
        }
        Log.d(TAG, "calculateMA OK: period=" + period + " size=" + size);
        MAIN_HANDLER.post(() -> callback.onResult(ma));
    }

    // ──────────────────────────────────────────────
    // 量比计算
    // - 量比 = 当前总成交量 / (过去5日平均每分钟成交量 × 当前已开盘分钟数)
    // - 简化计算: 当日成交量 / 过去5日平均成交量
    // ──────────────────────────────────────────────

    public static void calculateVolumeRatio(List<KlineData> dailyKlines,
                                            double todayVolume,
                                            Callback<Double> callback) {
        double ratio = 0;
        if (dailyKlines != null && dailyKlines.size() >= 5 && todayVolume > 0) {
            double sum = 0;
            int count = Math.min(dailyKlines.size(), 20);
            for (int i = 0; i < count; i++) {
                sum += dailyKlines.get(i).getVolume();
            }
            double avgVolume = sum / count;
            ratio = avgVolume > 0 ? todayVolume / avgVolume : 0;
            Log.d(TAG, "calculateVolumeRatio: todayVol=" + todayVolume
                    + " avgVol(5d)=" + avgVolume + " ratio=" + ratio);
        } else {
            Log.w(TAG, "calculateVolumeRatio insufficient data: klines="
                    + (dailyKlines != null ? dailyKlines.size() : 0)
                    + " todayVol=" + todayVolume);
        }
        double finalRatio = ratio;
        MAIN_HANDLER.post(() -> callback.onResult(finalRatio));
    }

    // ──────────────────────────────────────────────
    // 股票/ETF 列表查询
    // ──────────────────────────────────────────────

    public static void fetchStockList(Callback<List<Stock>> callback) {
        fetchSinaPages("hs_a", Stock.TYPE_STOCK, 1, callback);
    }

    public static void fetchEtfList(Callback<List<Stock>> callback) {
        fetchSinaPages("etf_hq_fund", Stock.TYPE_ETF, 1, callback);
    }

    // ──────────────────────────────────────────────
    // 搜索建议
    // ──────────────────────────────────────────────

    private static final String SEARCH_URL = "https://searchadapter.eastmoney.com/api/suggest/get";

    private static List<Stock> allStocksCache;
    private static boolean allStocksLoading = false;
    private static int loadFailCount = 0;
    private static final int MAX_LOAD_RETRIES = 3;
    private static final List<Callback<List<Stock>>> pendingStockCallbacks = new ArrayList<>();

    public static void searchSuggest(String keyword, Callback<List<Stock>> callback) {
        if (keyword == null || keyword.trim().isEmpty()) {
            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            return;
        }
        final String kw = keyword.trim();

        searchFromEastMoney(kw, result -> {
            if (!result.isEmpty()) {
                callback.onResult(result);
                return;
            }
            if (allStocksCache != null) {
                List<Stock> filtered = filterStocks(allStocksCache, kw.toLowerCase());
                callback.onResult(filtered);
                return;
            }
            callback.onResult(new ArrayList<>());
        });
    }

    private static void searchFromEastMoney(String keyword, Callback<List<Stock>> callback) {
        String encoded;
        try {
            encoded = URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e) {
            MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            return;
        }
        String url = SEARCH_URL + "?type=14&input=" + encoded + "&count=20";
        Log.d(TAG, "searchFromEastMoney url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_EM)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "searchFromEastMoney onFailure: " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                        return;
                    }
                    String respBody = response.body().string();
                    Log.d(TAG, "searchFromEastMoney raw: " + truncate(respBody, 300));

                    JsonObject json = GSON.fromJson(respBody, JsonObject.class);
                    List<Stock> stocks = new ArrayList<>();

                    if (json.has("QuotationCodeTable") && json.get("QuotationCodeTable").isJsonObject()) {
                        JsonObject table = json.getAsJsonObject("QuotationCodeTable");
                        if (table.has("Data") && table.get("Data").isJsonArray()) {
                            JsonArray arr = table.getAsJsonArray("Data");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonObject item = arr.get(i).getAsJsonObject();
                                String code = getJsonStr(item, "Code");
                                String name = getJsonStr(item, "Name");
                                String classify = getJsonStr(item, "Classify");
                                if (code.isEmpty() || name.isEmpty()) continue;
                                if (!"AStock".equals(classify) && !"Fund".equals(classify)) continue;
                                String stockType = "Fund".equals(classify) ? Stock.TYPE_ETF : Stock.TYPE_STOCK;
                                stocks.add(new Stock(code, name, stockType));
                                if (stocks.size() >= 15) break;
                            }
                        }
                    }
                    Log.i(TAG, "searchFromEastMoney OK: keyword=" + keyword + " count=" + stocks.size());
                    MAIN_HANDLER.post(() -> callback.onResult(stocks));
                } catch (Exception e) {
                    Log.e(TAG, "searchFromEastMoney parse error: " + e.getMessage());
                    MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                }
            }
        });
    }

    private static void loadAllStocks(Callback<List<Stock>> callback, String kw) {
        if (allStocksLoading) {
            pendingStockCallbacks.add(list -> callback.onResult(filterStocks(list, kw)));
            return;
        }

        if (loadFailCount >= MAX_LOAD_RETRIES) {
            List<Stock> cached = stockListCache != null ? stockListCache.load() : null;
            if (cached != null && !cached.isEmpty()) {
                allStocksCache = cached;
                loadFailCount = 0;
                MAIN_HANDLER.post(() -> callback.onResult(filterStocks(cached, kw)));
            } else {
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
            }
            return;
        }

        allStocksLoading = true;
        fetchAllStocksInternal(list -> {
            if (list.isEmpty()) {
                loadFailCount++;
                allStocksLoading = false;
                MAIN_HANDLER.post(() -> callback.onResult(new ArrayList<>()));
                for (Callback<List<Stock>> pending : pendingStockCallbacks) {
                    pending.onResult(new ArrayList<>());
                }
                pendingStockCallbacks.clear();
                return;
            }

            allStocksCache = list;
            loadFailCount = 0;
            allStocksLoading = false;
            if (stockListCache != null) {
                stockListCache.save(list);
            }
            callback.onResult(filterStocks(list, kw));
            for (Callback<List<Stock>> pending : pendingStockCallbacks) {
                pending.onResult(list);
            }
            pendingStockCallbacks.clear();
        });
    }

    private static List<Stock> filterStocks(List<Stock> stocks, String keyword) {
        List<Stock> result = new ArrayList<>();
        for (Stock s : stocks) {
            if (s.getCode().toLowerCase().contains(keyword)
                    || s.getName().toLowerCase().contains(keyword)) {
                result.add(s);
            }
            if (result.size() >= 50) break;
        }
        return result;
    }

    public static void refreshStockLists() {
        new Thread(() -> {
            fetchAllStocksInternal(list -> {
                if (!list.isEmpty()) {
                    allStocksCache = list;
                    loadFailCount = 0;
                    if (stockListCache != null) {
                        stockListCache.save(list);
                    }
                    Log.i(TAG, "refreshStockLists OK: size=" + list.size());
                } else {
                    Log.w(TAG, "refreshStockLists empty result");
                }
            });
        }).start();
    }

    private static void fetchAllStocksInternal(Callback<List<Stock>> callback) {
        final List<Stock> allStocks = new ArrayList<>();
        final int[] doneCount = {0};
        final Runnable checkAllDone = () -> {
            if (doneCount[0] == 2) {
                Log.i(TAG, "fetchAllStocks combined OK: size=" + allStocks.size());
                MAIN_HANDLER.post(() -> callback.onResult(allStocks));
            }
        };

        fetchSinaPages("hs_a", Stock.TYPE_STOCK, 1, list -> {
            synchronized (allStocks) { allStocks.addAll(list); }
            synchronized (doneCount) { doneCount[0]++; }
            checkAllDone.run();
        });

        fetchSinaPages("etf_hq_fund", Stock.TYPE_ETF, 1, list -> {
            synchronized (allStocks) { allStocks.addAll(list); }
            synchronized (doneCount) { doneCount[0]++; }
            checkAllDone.run();
        });
    }

    private static void fetchSinaPages(String node, String type, int page,
                                       Callback<List<Stock>> callback) {
        final int PAGE_SIZE = 1000;
        final int MAX_PAGES = 15;
        final List<Stock> allStocks = new ArrayList<>();
        fetchSinaPage(node, type, page, PAGE_SIZE, MAX_PAGES, allStocks, callback);
    }

    private static void fetchSinaPage(String node, String type, int page, int pageSize,
                                      int maxPages, List<Stock> accumulated,
                                      Callback<List<Stock>> callback) {
        if (page > maxPages) {
            Log.w(TAG, "fetchSinaPage exceeded max pages: node=" + node);
            MAIN_HANDLER.post(() -> callback.onResult(accumulated));
            return;
        }

        String url = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php"
                + "/Market_Center.getHQNodeData"
                + "?page=" + page + "&num=" + pageSize
                + "&sort=symbol&asc=1&node=" + node + "&symbol=";

        Log.d(TAG, "fetchSinaPage page=" + page + " node=" + node);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UA)
                .addHeader("Referer", REFERER_SINA)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchSinaPage onFailure page=" + page + ": " + e.getMessage());
                MAIN_HANDLER.post(() -> callback.onResult(accumulated));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "fetchSinaPage response code=" + response.code());
                        MAIN_HANDLER.post(() -> callback.onResult(accumulated));
                        return;
                    }
                    String respBody = response.body().string();
                    JsonArray arr = GSON.fromJson(respBody, JsonArray.class);

                    int count = 0;
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject item = arr.get(i).getAsJsonObject();
                        String code = getJsonStr(item, "code");
                        String name = getJsonStr(item, "name");
                        if (!code.isEmpty() && !name.isEmpty()) {
                            accumulated.add(new Stock(code, name, type));
                            count++;
                        }
                    }

                    Log.d(TAG, "fetchSinaPage page=" + page + " count=" + count
                            + " accumulated=" + accumulated.size());

                    if (count >= pageSize) {
                        fetchSinaPage(node, type, page + 1, pageSize,
                                maxPages, accumulated, callback);
                    } else {
                        Log.i(TAG, "fetchSinaPages done: node=" + node
                                + " total=" + accumulated.size());
                        MAIN_HANDLER.post(() -> callback.onResult(accumulated));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchSinaPage parse error page=" + page + ": " + e.getMessage());
                    MAIN_HANDLER.post(() -> callback.onResult(accumulated));
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    // 辅助方法
    // ──────────────────────────────────────────────

    private static String getSecid(String code) {
        if (code.equals("000001") || code.equals("000016") || code.equals("000300")
                || code.equals("000688") || code.equals("000852")) {
            return "1." + code;
        }
        if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
            return "1." + code;
        } else if (code.startsWith("8") || code.startsWith("4")) {
            return "2." + code;
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
            double volume = parseDouble(parts[8]);
            double amount = parseDouble(parts[9]) / 10000.0;

            double change = price - preClose;
            double pctChg = preClose > 0 ? change / preClose * 100.0 : 0;

            return new RealtimeQuote.Builder()
                    .name(name).code(code)
                    .price(price).high(high).low(low).open(open)
                    .volume(volume).amount(amount)
                    .pctChg(pctChg).change(change).preClose(preClose)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "parseSinaResponse error: " + e.getMessage(), e);
            return new RealtimeQuote();
        }
    }

    private static double getJsonDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try { return obj.get(key).getAsDouble(); } catch (Exception ignored) {}
        }
        return 0;
    }

    private static String getJsonStr(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try { return obj.get(key).getAsString(); } catch (Exception ignored) {}
        }
        return "";
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
