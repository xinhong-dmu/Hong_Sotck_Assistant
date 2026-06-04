package com.hong.xin.stock.data.api;

import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.RealtimeQuote;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockDataCache {

    private static final int MAX_KLINE_ENTRIES = 30;
    private static final int MAX_QUOTE_ENTRIES = 50;

    private static final long KLINE_TTL_MS = 5 * 60 * 1000L;
    private static final long QUOTE_TTL_MS = 3 * 1000L;

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

    private StockDataCache() {}

    private String klineKey(String code, int days) {
        return code + "_" + days;
    }

    public synchronized List<KlineData> getKline(String code, int days) {
        CacheEntry<List<KlineData>> entry = klineCache.get(klineKey(code, days));
        if (entry != null && !entry.isExpired(KLINE_TTL_MS)) {
            return entry.data;
        }
        return null;
    }

    public synchronized List<KlineData> getKlineStale(String code, int days) {
        CacheEntry<List<KlineData>> entry = klineCache.get(klineKey(code, days));
        if (entry != null) {
            return entry.data;
        }
        return null;
    }

    public synchronized void putKline(String code, int days, List<KlineData> data) {
        klineCache.put(klineKey(code, days), new CacheEntry<>(data));
    }

    public synchronized RealtimeQuote getQuote(String code) {
        CacheEntry<RealtimeQuote> entry = quoteCache.get(code);
        if (entry != null && !entry.isExpired(QUOTE_TTL_MS)) {
            return entry.data;
        }
        return null;
    }

    public synchronized RealtimeQuote getQuoteStale(String code) {
        CacheEntry<RealtimeQuote> entry = quoteCache.get(code);
        if (entry != null) {
            return entry.data;
        }
        return null;
    }

    public synchronized void putQuote(String code, RealtimeQuote data) {
        quoteCache.put(code, new CacheEntry<>(data));
    }

    public synchronized void clear() {
        klineCache.clear();
        quoteCache.clear();
    }

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
