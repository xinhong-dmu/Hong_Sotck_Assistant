package com.hong.xin.stock.data.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockNewsApi {

    private static final String TAG = "StockNewsApi";
    private static final OkHttpClient CLIENT = HttpClientFactory.getClient();
    private static final Gson GSON = new Gson();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void fetchStockNews(String stockCode, String stockName, int count, Callback<List<NewsItem>> callback) {
        new Thread(() -> {
            try {
                List<NewsItem> news = fetchFromSinaRoll(stockName, stockCode, count * 2, false);
                final List<NewsItem> result = news.size() > count ? news.subList(0, count) : news;
                MAIN_HANDLER.post(() -> callback.onResult(result));
            } catch (Exception e) {
                Log.e(TAG, "fetchStockNews err: " + e.getMessage());
                final List<NewsItem> fb = new ArrayList<>();
                fb.add(new NewsItem(stockName + ": 暂无相关新闻", "", ""));
                MAIN_HANDLER.post(() -> callback.onResult(fb));
            }
        }).start();
    }

    public static void fetchMarketNews(int count, Callback<List<NewsItem>> callback) {
        new Thread(() -> {
            try {
                List<NewsItem> news = fetchMarketFromSinaRoll(count);
                final List<NewsItem> result = news.size() > count ? news.subList(0, count) : news;
                MAIN_HANDLER.post(() -> callback.onResult(result));
            } catch (Exception e) {
                Log.e(TAG, "fetchMarketNews err: " + e.getMessage());
                final List<NewsItem> fb = new ArrayList<>();
                fb.add(new NewsItem("暂无市场要闻", "", ""));
                MAIN_HANDLER.post(() -> callback.onResult(fb));
            }
        }).start();
    }

    private static List<NewsItem> fetchMarketFromSinaRoll(int count) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Log.d(TAG, "today: " + today);

        List<NewsItem> all = new ArrayList<>();

        all.addAll(fetchFromSinaRoll("", "", count * 2, true));
        if (all.size() >= count) return all;

        all.addAll(fetchFromSinaRoll("", "", count * 2, false));

        return all;
    }

    private static List<NewsItem> fetchFromSinaRoll(String filterName, String filterCode, int count, boolean todayOnly) {
        List<NewsItem> all = new ArrayList<>();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        try {
            int fetchCount = Math.max(count * 2, 10);
            String url = "https://feed.mix.sina.com.cn/api/roll/get"
                    + "?pageid=153&lid=2509&k=&num=" + fetchCount + "&page=1";

            Log.d(TAG, "fetch url: " + url);
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Referer", "https://finance.sina.com.cn/")
                    .build();

            Response resp = CLIENT.newCall(req).execute();
            Log.d(TAG, "resp code: " + resp.code());
            if (!resp.isSuccessful() || resp.body() == null) {
                resp.close();
                return all;
            }

            String text = resp.body().string();
            resp.close();

            JsonObject obj = GSON.fromJson(text, JsonObject.class);
            if (!obj.has("result") || !obj.get("result").isJsonObject()) {
                return all;
            }
            JsonObject result = obj.getAsJsonObject("result");
            if (!result.has("data") || !result.get("data").isJsonArray()) {
                return all;
            }
            JsonArray arr = result.getAsJsonArray("data");
            Log.d(TAG, "data count: " + arr.size());

            boolean doFilter = !filterName.isEmpty() || !filterCode.isEmpty();
            String kw1 = filterName.toLowerCase();
            String kw2 = filterCode.toLowerCase();

            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                String title = "";
                if (item.has("title") && !item.get("title").isJsonNull()) {
                    title = item.get("title").getAsString();
                }
                if (title.isEmpty()) continue;

                String date = "";
                if (item.has("ctime") && !item.get("ctime").isJsonNull()) {
                    date = item.get("ctime").getAsString();
                    if (date.length() > 10) date = date.substring(0, 10);
                }

                if (todayOnly && !date.equals(today)) continue;

                if (doFilter) {
                    String t = title.toLowerCase();
                    if (!t.contains(kw1) && !t.contains(kw2)) continue;
                }

                String urlStr = "";
                if (item.has("url") && !item.get("url").isJsonNull()) {
                    urlStr = item.get("url").getAsString();
                }
                all.add(new NewsItem(title, date, urlStr));

                if (all.size() >= count) break;
            }
        } catch (Exception e) {
            Log.e(TAG, "fetch err: " + e.getMessage(), e);
        }
        Log.i(TAG, "returning " + all.size() + " items todayOnly=" + todayOnly);
        return all;
    }

    public static class NewsItem {
        private final String title;
        private final String date;
        private final String url;

        public NewsItem(String title, String date, String url) {
            this.title = title;
            this.date = date;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getDate() { return date; }
        public String getUrl() { return url; }
    }
}
