package com.hong.xin.stock.data.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
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
            List<NewsItem> all = new ArrayList<>();

            all.addAll(fetchFromEastMoneySearch(stockCode, count));
            if (all.size() < count) {
                all.addAll(fetchFromEastMoneySearch(stockName, count));
            }
            if (all.size() < count) {
                all.addAll(fetchFromSinaRoll(stockName, stockCode, count * 2));
            }

            final List<NewsItem> result = all.size() > count ? all.subList(0, count) : all;
            MAIN_HANDLER.post(() -> callback.onResult(result));
        }).start();
    }

    private static List<NewsItem> fetchFromEastMoneySearch(String keyword, int count) {
        List<NewsItem> list = new ArrayList<>();
        try {
            String encodedKw = URLEncoder.encode(keyword, "UTF-8");
            String param = "{\"uid\":\"\",\"keyword\":\"" + keyword + "\","
                    + "\"type\":[\"cmsArticleWebOld\"],\"client\":\"web\",\"clientType\":\"web\","
                    + "\"clientVersion\":\"curr\",\"param\":{\"cmsArticleWebOld\":{"
                    + "\"searchScope\":\"default\",\"sort\":\"default\",\"pageIndex\":1,"
                    + "\"pageSize\":" + count + ",\"preTag\":\"\",\"postTag\":\"\"}}}";
            String encodedParam = URLEncoder.encode(param, "UTF-8");
            String url = "https://search-api-web.eastmoney.com/search/jsonp?cb=jQuery&param=" + encodedParam;

            Log.d(TAG, "EastMoney search url: " + url);
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Referer", "https://www.eastmoney.com/")
                    .build();

            Response resp = CLIENT.newCall(req).execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                resp.close();
                return list;
            }

            String text = resp.body().string();
            resp.close();

            int start = text.indexOf("(");
            int end = text.lastIndexOf(")");
            if (start >= 0 && end > start) {
                String json = text.substring(start + 1, end);
                list.addAll(parseEastMoneySearchResponse(json));
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchFromEastMoneySearch err: " + e.getMessage(), e);
        }
        return list;
    }

    private static List<NewsItem> parseEastMoneySearchResponse(String json) {
        List<NewsItem> list = new ArrayList<>();
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj == null) return list;
            JsonObject result = obj.getAsJsonObject("result");
            if (result == null) return list;
            JsonArray arr = result.getAsJsonArray("cmsArticleWebOld");
            if (arr == null) return list;

            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                String title = item.has("title") && !item.get("title").isJsonNull()
                        ? item.get("title").getAsString() : "";
                String date = item.has("date") && !item.get("date").isJsonNull()
                        ? item.get("date").getAsString() : "";
                String url = item.has("url") && !item.get("url").isJsonNull()
                        ? item.get("url").getAsString() : "";
                if (title.isEmpty()) continue;
                list.add(new NewsItem(title, date, url));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseEastMoney err: " + e.getMessage(), e);
        }
        return list;
    }

    private static List<NewsItem> fetchFromSinaRoll(String filterName, String filterCode, int count) {
        List<NewsItem> all = new ArrayList<>();
        try {
            int fetchCount = Math.max(count * 2, 10);
            String url = "https://feed.mix.sina.com.cn/api/roll/get"
                    + "?pageid=153&lid=2509&k=&num=" + fetchCount + "&page=1";

            Log.d(TAG, "Sina roll url: " + url);
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Referer", "https://finance.sina.com.cn/")
                    .build();

            Response resp = CLIENT.newCall(req).execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                resp.close();
                return all;
            }

            String text = resp.body().string();
            resp.close();

            JsonObject obj = GSON.fromJson(text, JsonObject.class);
            if (obj == null || !obj.has("result") || !obj.get("result").isJsonObject()) return all;
            JsonObject result = obj.getAsJsonObject("result");
            if (!result.has("data") || !result.get("data").isJsonArray()) return all;
            JsonArray arr = result.getAsJsonArray("data");

            String kw1 = filterName.toLowerCase();
            String kw2 = filterCode.toLowerCase();

            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                String title = item.has("title") && !item.get("title").isJsonNull()
                        ? item.get("title").getAsString() : "";
                if (title.isEmpty()) continue;

                String t = title.toLowerCase();
                if (!t.contains(kw1) && !t.contains(kw2)) continue;

                String date = item.has("ctime") && !item.get("ctime").isJsonNull()
                        ? item.get("ctime").getAsString() : "";
                if (date.length() > 10) date = date.substring(0, 10);
                String urlStr = item.has("url") && !item.get("url").isJsonNull()
                        ? item.get("url").getAsString() : "";

                all.add(new NewsItem(title, date, urlStr));
                if (all.size() >= count) break;
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchFromSinaRoll err: " + e.getMessage(), e);
        }
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
