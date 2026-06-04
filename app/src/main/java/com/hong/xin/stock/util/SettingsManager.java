package com.hong.xin.stock.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hong.xin.stock.data.model.TradePreset;

import java.util.ArrayList;
import java.util.List;

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
    private static final String KEY_ANALYSIS_REQUESTED = "analysis_requested";
    private static final String KEY_DEEPSEEK_MODEL = "deepseek_model";
    private static final String KEY_PENDING_ALERT_DIALOG = "pending_alert_dialog";
    private static final String KEY_TARGET_ALERTED = "target_alerted";
    private static final String KEY_DRAWDOWN_ALERTED = "drawdown_alerted";
    private static final String KEY_PRESETS = "trade_presets";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

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

    public void setHighestPrice(double price) { prefs.edit().putFloat(KEY_HIGHEST_PRICE, (float) price).apply(); }
    public double getHighestPrice() { return getSafeFloat(KEY_HIGHEST_PRICE, 0); }

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

    public void setAnalysisRequested(boolean requested) { prefs.edit().putBoolean(KEY_ANALYSIS_REQUESTED, requested).apply(); }
    public boolean isAnalysisRequested() { return prefs.getBoolean(KEY_ANALYSIS_REQUESTED, false); }

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

    public void setPendingAlertDialog(String message) { prefs.edit().putString(KEY_PENDING_ALERT_DIALOG, message).apply(); }
    public String getPendingAlertDialog() { return prefs.getString(KEY_PENDING_ALERT_DIALOG, null); }
    public boolean hasPendingAlertDialog() { return prefs.contains(KEY_PENDING_ALERT_DIALOG); }
    public void clearPendingAlertDialog() { prefs.edit().remove(KEY_PENDING_ALERT_DIALOG).apply(); }

    public void setTargetAlerted(boolean alerted) { prefs.edit().putBoolean(KEY_TARGET_ALERTED, alerted).apply(); }
    public boolean getTargetAlerted() { return prefs.getBoolean(KEY_TARGET_ALERTED, false); }

    public void setDrawdownCriticalAlerted(boolean alerted) { prefs.edit().putBoolean(KEY_DRAWDOWN_ALERTED, alerted).apply(); }
    public boolean getDrawdownCriticalAlerted() { return prefs.getBoolean(KEY_DRAWDOWN_ALERTED, false); }

    public void clearDashboard() {
        prefs.edit()
                .putBoolean(KEY_DASHBOARD_VISIBLE, false)
                .putBoolean(KEY_IS_ACTIVE, false)
                .putFloat(KEY_HIGHEST_PRICE, 0)
                .putFloat(KEY_DEFENSE_LINE, 0)
                .putFloat(KEY_HARD_STOP_LINE, 0)
                .putBoolean(KEY_TARGET_ALERTED, false)
                .putBoolean(KEY_DRAWDOWN_ALERTED, false)
                .apply();
    }

    public List<TradePreset> getPresets() {
        String json = prefs.getString(KEY_PRESETS, "[]");
        try {
            java.lang.reflect.Type type = new TypeToken<List<TradePreset>>(){}.getType();
            List<TradePreset> presets = gson.fromJson(json, type);
            return presets != null ? presets : new ArrayList<TradePreset>();
        } catch (Exception e) {
            return new ArrayList<TradePreset>();
        }
    }

    public void savePreset(TradePreset preset) {
        List<TradePreset> presets = getPresets();
        presets.add(0, preset);
        prefs.edit().putString(KEY_PRESETS, gson.toJson(presets)).apply();
    }

    public void deletePreset(String id) {
        List<TradePreset> presets = getPresets();
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).getId().equals(id)) {
                presets.remove(i);
                break;
            }
        }
        prefs.edit().putString(KEY_PRESETS, gson.toJson(presets)).apply();
    }

    public void loadPresetParams(TradePreset preset) {
        prefs.edit()
                .putString(KEY_STOCK_NAME, preset.getStockName())
                .putString(KEY_STOCK_CODE, preset.getStockCode())
                .putFloat(KEY_BUY_PRICE, (float) preset.getBuyPrice())
                .putString(KEY_BUY_DATE, preset.getBuyDate())
                .putFloat(KEY_STOP_LOSS, (float) preset.getStopLossPercent())
                .putFloat(KEY_TARGET_PROFIT, (float) preset.getTargetProfitPercent())
                .putFloat(KEY_TRAILING, (float) preset.getTrailingPercent())
                .putBoolean(KEY_USE_GRADED, preset.isUseGraded())
                .apply();
    }
}
