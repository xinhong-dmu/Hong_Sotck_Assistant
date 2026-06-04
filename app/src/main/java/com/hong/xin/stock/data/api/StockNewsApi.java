package com.hong.xin.stock.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockNewsApi {

    private static final OkHttpClient CLIENT = HttpClientFactory.getClient();
    private static final Gson GSON = new Gson();

    public static List<String> getStockNewsSync(String stockCode, String stockName, int count) {
        List<String> news = new ArrayList<>();
        try {
            String newsUrl = "https://search-api-web.eastmoney.com/search/jsonp"
                    + "?cb=jQuery&param=" + stockName
                    + "&type=14&client=web&pageIndex=1&pageSize=" + count;
            Request newsReq = new Request.Builder()
                    .url(newsUrl)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://so.eastmoney.com/")
                    .build();
            Response resp = CLIENT.newCall(newsReq).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                String text = resp.body().string();
                extractNews(text, news, count);
            }
        } catch (IOException ignored) {
        }

        if (news.isEmpty()) {
            news.add(stockName + ": 暂无相关新闻数据");
        }
        return news;
    }

    public static List<String> getPolicyNewsSync(int count) {
        List<String> news = new ArrayList<>();
        try {
            String url = "https://search-api-web.eastmoney.com/search/jsonp"
                    + "?cb=jQuery&param=A股 政策 监管"
                    + "&type=14&client=web&pageIndex=1&pageSize=" + count;
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Referer", "https://so.eastmoney.com/")
                    .build();
            Response resp = CLIENT.newCall(req).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                String text = resp.body().string();
                extractNews(text, news, count);
            }
        } catch (IOException ignored) {
        }

        if (news.isEmpty()) {
            news.add("暂无政策动态数据");
        }
        return news;
    }

    private static String getSecid(String code) {
        if (code.startsWith("6") || code.startsWith("9")) {
            return "1." + code;
        } else {
            return "0." + code;
        }
    }

    private static void extractNews(String jsonpText, List<String> result, int maxCount) {
        try {
            String json = jsonpText;
            int start = json.indexOf('(');
            int end = json.lastIndexOf(')');
            if (start >= 0 && end > start) {
                json = json.substring(start + 1, end);
            }
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.has("data") && obj.get("data").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("data");
                for (int i = 0; i < Math.min(arr.size(), maxCount); i++) {
                    JsonObject item = arr.get(i).getAsJsonObject();
                    String title = item.has("title") ? item.get("title").getAsString() : "";
                    String date = item.has("date") ? item.get("date").getAsString() : "";
                    result.add(date + " " + title);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
