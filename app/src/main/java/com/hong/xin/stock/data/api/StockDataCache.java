package com.hong.xin.stock.data.api;

import android.util.Log;

import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;
import com.hong.xin.stock.data.model.RealtimeQuote;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockDataCache {

    private static final String TAG = "StockDataCache";

    private static final int MAX_KLINE_ENTRIES = 30;
    private static final int MAX_QUOTE_ENTRIES = 50;
    private static final int MAX_MINUTE_ENTRIES = 20;

    private static final long KLINE_TTL_MS = 5 * 60 * 1000L;
    private static final long QUOTE_TTL_MS = 3 * 1000L;
    private static final long MINUTE_TTL_MS = 60 * 1000L;

    private final Map<String, CacheEntry<List<KlineData>>> klineCache =
            new LinkedHashMap<String, CacheEntry<List<KlineData>>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<List<KlineData>>> eldest) {
                    return size() > MAX_KLINE_ENTRIES;
                }
            };

    private final Map<String, CacheEntry<RealtimeQuote>> quoteCache =
            new LinkedHashMap<String, CacheEntry<RealtimeQuote>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<RealtimeQuote>> eldest) {
                    return size() > MAX_QUOTE_ENTRIES;
                }
            };

    private final Map<String, CacheEntry<List<MinuteLineData>>> minuteCache =
            new LinkedHashMap<String, CacheEntry<List<MinuteLineData>>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<List<MinuteLineData>>> eldest) {
                    return size() > MAX_MINUTE_ENTRIES;
                }
            };

    private static volatile StockDataCache instance;

    public static StockDataCache getInstance() {
        if (instance == null) {
            synchronized (StockDataCache.class) {
                if (instance == null) {
                    instance = new StockDataCache();
                }
            }
        }
        return instance;
    }

    private StockDataCache() {
        Log.d(TAG, "StockDataCache initialized");
    }

    // ── K-line Cache ──

    public synchronized List<KlineData> getKline(String code, String cacheKey) {
        CacheEntry<List<KlineData>> entry = klineCache.get(cacheKey);
        if (entry != null && !entry.isExpired(KLINE_TTL_MS)) {
            Log.d(TAG, "getKline hit: " + cacheKey);
            return entry.data;
        }
        Log.d(TAG, "getKline miss: " + cacheKey);
        return null;
    }

    public synchronized List<KlineData> getKlineStale(String code, String cacheKey) {
        CacheEntry<List<KlineData>> entry = klineCache.get(cacheKey);
        if (entry != null) {
            Log.d(TAG, "getKlineStale hit: " + cacheKey);
            return entry.data;
        }
        return null;
    }

    public synchronized void putKline(String code, String cacheKey, List<KlineData> data) {
        klineCache.put(cacheKey, new CacheEntry<>(data));
        Log.d(TAG, "putKline: " + cacheKey + " size=" + data.size()
                + " cacheSize=" + klineCache.size());
    }

    // ── Quote Cache ──

    public synchronized RealtimeQuote getQuote(String code) {
        CacheEntry<RealtimeQuote> entry = quoteCache.get(code);
        if (entry != null && !entry.isExpired(QUOTE_TTL_MS)) {
            Log.d(TAG, "getQuote hit: " + code);
            return entry.data;
        }
        return null;
    }

    public synchronized RealtimeQuote getQuoteStale(String code) {
        CacheEntry<RealtimeQuote> entry = quoteCache.get(code);
        if (entry != null) {
            Log.d(TAG, "getQuoteStale hit: " + code);
            return entry.data;
        }
        return null;
    }

    public synchronized void putQuote(String code, RealtimeQuote data) {
        quoteCache.put(code, new CacheEntry<>(data));
        Log.d(TAG, "putQuote: " + code + " price=" + data.getPrice()
                + " cacheSize=" + quoteCache.size());
    }

    // ── Minute-line Cache ──

    public synchronized List<MinuteLineData> getMinuteLine(String code, String cacheKey) {
        CacheEntry<List<MinuteLineData>> entry = minuteCache.get(cacheKey);
        if (entry != null && !entry.isExpired(MINUTE_TTL_MS)) {
            Log.d(TAG, "getMinuteLine hit: " + cacheKey);
            return entry.data;
        }
        return null;
    }

    public synchronized List<MinuteLineData> getMinuteLineStale(String code, String cacheKey) {
        CacheEntry<List<MinuteLineData>> entry = minuteCache.get(cacheKey);
        if (entry != null) {
            Log.d(TAG, "getMinuteLineStale hit: " + cacheKey);
            return entry.data;
        }
        return null;
    }

    public synchronized void putMinuteLine(String code, String cacheKey, List<MinuteLineData> data) {
        minuteCache.put(cacheKey, new CacheEntry<>(data));
        Log.d(TAG, "putMinuteLine: " + cacheKey + " size=" + data.size()
                + " cacheSize=" + minuteCache.size());
    }

    // ── Clear ──

    public synchronized void clear() {
        klineCache.clear();
        quoteCache.clear();
        minuteCache.clear();
        Log.d(TAG, "All caches cleared");
    }

    public synchronized void clearQuote(String code) {
        quoteCache.remove(code);
        Log.d(TAG, "clearQuote: " + code);
    }

    // ── Cache Entry ──

    private static class CacheEntry<T> {
        final T data;
        final long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
}
