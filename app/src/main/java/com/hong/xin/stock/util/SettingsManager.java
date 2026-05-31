package com.hong.xin.stock.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREF_NAME = "stock_settings";
    private static final String KEY_API_KEY = "deepseek_api_key";
    private static final String KEY_STOCK_NAME = "last_stock_name";
    private static final String KEY_STOCK_CODE = "last_stock_code";
    private static final String KEY_BUY_PRICE = "last_buy_price";
    private static final String KEY_BUY_DATE = "last_buy_date";
    private static final String KEY_STOP_LOSS = "stop_loss_percent";
    private static final String KEY_TARGET_PROFIT = "target_profit_percent";
    private static final String KEY_TRAILING = "trailing_percent";
    private static final String KEY_USE_GRADED = "use_graded";
    private static final String KEY_CURRENT_PRICE = "current_price";
    private static final String KEY_DASHBOARD_VISIBLE = "dashboard_visible";
    private static final String KEY_HIGHEST_PRICE = "highest_price";
    private static final String KEY_DEFENSE_LINE = "defense_line";
    private static final String KEY_HARD_STOP_LINE = "hard_stop_line";
    private static final String KEY_IS_ACTIVE = "is_active";
    private static final String KEY_MESSAGES = "analysis_messages";
    private static final String KEY_KLINE_DAYS = "kline_days";
    private static final String KEY_IS_ANALYZING = "is_analyzing";
    private static final String KEY_DEEPSEEK_MODEL = "deepseek_model";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setApiKey(String key) { prefs.edit().putString(KEY_API_KEY, key).apply(); }
    public String getApiKey() { return prefs.getString(KEY_API_KEY, ""); }

    public void setDeepSeekModel(String model) { prefs.edit().putString(KEY_DEEPSEEK_MODEL, model).apply(); }
    public String getDeepSeekModel() { return prefs.getString(KEY_DEEPSEEK_MODEL, "deepseek-chat"); }

    public void setLastStockName(String name) { prefs.edit().putString(KEY_STOCK_NAME, name).apply(); }
    public String getLastStockName() { return prefs.getString(KEY_STOCK_NAME, ""); }

    public void setLastStockCode(String code) { prefs.edit().putString(KEY_STOCK_CODE, code).apply(); }
    public String getLastStockCode() { return prefs.getString(KEY_STOCK_CODE, ""); }

    public void setLastBuyPrice(double price) { prefs.edit().putFloat(KEY_BUY_PRICE, (float) price).apply(); }
    public double getLastBuyPrice() { return getSafeFloat(KEY_BUY_PRICE, 0); }

    public void setLastBuyDate(String date) { prefs.edit().putString(KEY_BUY_DATE, date).apply(); }
    public String getLastBuyDate() { return prefs.getString(KEY_BUY_DATE, ""); }

    public void setStopLossPercent(double pct) { prefs.edit().putFloat(KEY_STOP_LOSS, (float) pct).apply(); }
    public double getStopLossPercent() { return getSafeFloat(KEY_STOP_LOSS, 3); }

    public void setTargetProfitPercent(double pct) { prefs.edit().putFloat(KEY_TARGET_PROFIT, (float) pct).apply(); }
    public double getTargetProfitPercent() { return getSafeFloat(KEY_TARGET_PROFIT, 10); }

    public void setTrailingPercent(double pct) { prefs.edit().putFloat(KEY_TRAILING, (float) pct).apply(); }
    public double getTrailingPercent() { return getSafeFloat(KEY_TRAILING, 0); }

    public void setUseGraded(boolean use) { prefs.edit().putBoolean(KEY_USE_GRADED, use).apply(); }
    public boolean getUseGraded() { return prefs.getBoolean(KEY_USE_GRADED, false); }

    public void setCurrentPrice(double price) { prefs.edit().putFloat(KEY_CURRENT_PRICE, (float) price).apply(); }
    public double getCurrentPrice() { return getSafeFloat(KEY_CURRENT_PRICE, 0); }

    public void setDashboardVisible(boolean visible) { prefs.edit().putBoolean(KEY_DASHBOARD_VISIBLE, visible).apply(); }
    public boolean isDashboardVisible() { return prefs.getBoolean(KEY_DASHBOARD_VISIBLE, false); }

    public void setDefenseLine(double line) { prefs.edit().putFloat(KEY_DEFENSE_LINE, (float) line).apply(); }
    public double getDefenseLine() { return getSafeFloat(KEY_DEFENSE_LINE, 0); }

    public void setHardStopLine(double line) { prefs.edit().putFloat(KEY_HARD_STOP_LINE, (float) line).apply(); }
    public double getHardStopLine() { return getSafeFloat(KEY_HARD_STOP_LINE, 0); }

    public void setIsActive(boolean active) { prefs.edit().putBoolean(KEY_IS_ACTIVE, active).apply(); }
    public boolean getIsActive() { return prefs.getBoolean(KEY_IS_ACTIVE, false); }

    private double getSafeFloat(String key, double defaultValue) {
        try {
            return prefs.getFloat(key, (float) defaultValue);
        } catch (ClassCastException e) {
            try {
                String val = prefs.getString(key, null);
                if (val != null) return Double.parseDouble(val);
            } catch (Exception ignored) {}
            return defaultValue;
        }
    }

    public void setMessages(String json) { prefs.edit().putString(KEY_MESSAGES, json).apply(); }
    public String getMessages() { return prefs.getString(KEY_MESSAGES, ""); }

    public void setKlineDays(int days) { prefs.edit().putInt(KEY_KLINE_DAYS, days).apply(); }
    public int getKlineDays() { return prefs.getInt(KEY_KLINE_DAYS, 120); }

    public void setIsAnalyzing(boolean analyzing) { prefs.edit().putBoolean(KEY_IS_ANALYZING, analyzing).apply(); }
    public boolean getIsAnalyzing() { return prefs.getBoolean(KEY_IS_ANALYZING, false); }

    public void saveTradeParams(String name, String code, double buyPrice, String buyDate,
                                double stopLoss, double targetProfit, double trailing, boolean useGraded) {
        prefs.edit()
                .putString(KEY_STOCK_NAME, name)
                .putString(KEY_STOCK_CODE, code)
                .putFloat(KEY_BUY_PRICE, (float) buyPrice)
                .putString(KEY_BUY_DATE, buyDate)
                .putFloat(KEY_STOP_LOSS, (float) stopLoss)
                .putFloat(KEY_TARGET_PROFIT, (float) targetProfit)
                .putFloat(KEY_TRAILING, (float) trailing)
                .putBoolean(KEY_USE_GRADED, useGraded)
                .putBoolean(KEY_IS_ACTIVE, true)
                .apply();
    }

    public void clearDashboard() {
        prefs.edit()
                .putBoolean(KEY_DASHBOARD_VISIBLE, false)
                .putBoolean(KEY_IS_ACTIVE, false)
                .putFloat(KEY_HIGHEST_PRICE, 0)
                .putFloat(KEY_DEFENSE_LINE, 0)
                .putFloat(KEY_HARD_STOP_LINE, 0)
                .apply();
    }
}
