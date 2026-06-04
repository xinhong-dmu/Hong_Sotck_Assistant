package com.hong.xin.stock.ui.analysis;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.api.StockNewsApi;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.util.SettingsManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiAnalysisViewModel extends AndroidViewModel {

    private final MutableLiveData<AiAnalysisState> uiState = new MutableLiveData<>(AiAnalysisState.builder().build());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final SettingsManager settingsManager;
    private final Gson gson = new Gson();
    private boolean stopRequested = false;
    private List<DeepSeekApi.ChatMessage> conversationHistory = new ArrayList<>();

    public AiAnalysisViewModel(Application application) {
        super(application);
        this.settingsManager = new SettingsManager(application);

        String savedKey = settingsManager.getApiKey();
        String code = settingsManager.getLastStockCode();
        String name = settingsManager.getLastStockName();
        double buyPrice = settingsManager.getLastBuyPrice();
        String buyDate = settingsManager.getLastBuyDate();
        double stopLoss = settingsManager.getStopLossPercent();
        double targetProfit = settingsManager.getTargetProfitPercent();
        double trailing = settingsManager.getTrailingPercent();
        boolean useGraded = settingsManager.getUseGraded();
        int klineDays = settingsManager.getKlineDays();

        if (!savedKey.isEmpty()) {
            DeepSeekApi.setApiKey(savedKey);
        }

        String savedModel = settingsManager.getDeepSeekModel();
        if (!savedModel.isEmpty()) {
            DeepSeekApi.setModel(savedModel);
        }

        List<AiAnalysisState.ChatMessage> messages = loadMessages();

        uiState.setValue(AiAnalysisState.builder()
                .stockCode(code)
                .stockName(name)
                .buyPrice(buyPrice)
                .buyDate(buyDate)
                .klineDays(klineDays)
                .stopLossPercent(stopLoss)
                .targetProfitPercent(targetProfit)
                .trailingPercent(trailing)
                .useGraded(useGraded)
                .messages(messages)
                .build());
    }

    private List<AiAnalysisState.ChatMessage> loadMessages() {
        String json = settingsManager.getMessages();
        if (json.isEmpty()) return new ArrayList<>();
        try {
            java.lang.reflect.Type type = new TypeToken<List<AiAnalysisState.ChatMessage>>(){}.getType();
            List<AiAnalysisState.ChatMessage> msgs = gson.fromJson(json, type);
            return msgs != null ? msgs : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveMessages(List<AiAnalysisState.ChatMessage> messages) {
        settingsManager.setMessages(gson.toJson(messages));
    }

    public LiveData<AiAnalysisState> getUiState() {
        return uiState;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void setApiKey(String key) {
        settingsManager.setApiKey(key);
        DeepSeekApi.setApiKey(key);
    }

    public void updateStockInfo(String code, String name) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setLastStockCode(code);
        settingsManager.setLastStockName(name);
        uiState.setValue(state.copy().stockCode(code).stockName(name).build());
    }

    public void updateBuyPrice(double price) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setLastBuyPrice(price);
        uiState.setValue(state.copy().buyPrice(price).build());
    }

    public void updateBuyDate(String date) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setLastBuyDate(date);
        uiState.setValue(state.copy().buyDate(date).build());
    }

    public void updateKlineDays(int days) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setKlineDays(days);
        uiState.setValue(state.copy().klineDays(days).build());
    }

    public void updateStopLossPercent(double pct) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setStopLossPercent(pct);
        uiState.setValue(state.copy().stopLossPercent(pct).build());
    }

    public void updateTargetProfitPercent(double pct) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setTargetProfitPercent(pct);
        uiState.setValue(state.copy().targetProfitPercent(pct).build());
    }

    public void updateTrailingPercent(double pct) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setTrailingPercent(pct);
        uiState.setValue(state.copy().trailingPercent(pct).build());
    }

    public void loadFromSettings() {
        String code = settingsManager.getLastStockCode();
        String name = settingsManager.getLastStockName();
        double buyPrice = settingsManager.getLastBuyPrice();
        String buyDate = settingsManager.getLastBuyDate();
        int klineDays = settingsManager.getKlineDays();
        double stopLoss = settingsManager.getStopLossPercent();
        double targetProfit = settingsManager.getTargetProfitPercent();

        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy()
                .stockCode(code).stockName(name)
                .buyPrice(buyPrice).buyDate(buyDate).klineDays(klineDays)
                .stopLossPercent(stopLoss).targetProfitPercent(targetProfit)
                .build());
    }

    public void updateUseGraded(boolean use) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        settingsManager.setUseGraded(use);
        uiState.setValue(state.copy().useGraded(use).build());
    }

    public void startAnalysis() {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;

        if (settingsManager.getApiKey().isEmpty()) {
            toastMessage.setValue("请先输入 DeepSeek API Key");
            return;
        }
        if (state.getStockCode().isEmpty() || state.getStockName().isEmpty()) {
            toastMessage.setValue("请先输入股票代码和名称");
            return;
        }

        stopRequested = false;
        uiState.setValue(state.copy().isAnalyzing(true).build());
        settingsManager.setIsAnalyzing(true);

        addMessage(false, "正在获取K线数据...");

        EastMoneyApi.fetchKline(state.getStockCode(), state.getKlineDays(),
                new EastMoneyApi.Callback<List<KlineData>>() {
                    @Override
                    public void onResult(List<KlineData> klineList) {
                        if (stopRequested) {
                            finishAnalysis();
                            return;
                        }

                        if (klineList.isEmpty()) {
                            addMessage(false, "获取K线数据失败，请检查股票代码是否正确");
                            finishAnalysis();
                            return;
                        }

                        addMessage(false, String.format(Locale.CHINA,
                                "已获取 %d 条K线数据，正在抓取新闻...", klineList.size()));

                        String stockCode = uiState.getValue() != null ? uiState.getValue().getStockCode() : "";
                        String stockName = uiState.getValue() != null ? uiState.getValue().getStockName() : "";

                        new Thread(() -> {
                            List<String> news = StockNewsApi.getStockNewsSync(stockCode, stockName, 5);
                            List<String> policy = StockNewsApi.getPolicyNewsSync(3);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (stopRequested) {
                                    finishAnalysis();
                                    return;
                                }

                                addMessage(false, "正在调用DeepSeek AI进行分析...");
                                String prompt = buildAnalysisPrompt(klineList, news, policy);
                                Log.d("AiAnalysis", "AI Prompt:\n" + prompt);
                                callDeepSeek(prompt);
                            });
                        }).start();
                    }
                });
    }

    public void stopAnalysis() {
        stopRequested = true;
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy().isAnalyzing(false).build());
        settingsManager.setIsAnalyzing(false);
        addMessage(false, "分析已停止");
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        AiAnalysisState state = uiState.getValue();
        if (state == null) return;

        List<AiAnalysisState.ChatMessage> msgs = new ArrayList<>(state.getMessages());
        msgs.add(new AiAnalysisState.ChatMessage(true, text.trim()));
        msgs.add(new AiAnalysisState.ChatMessage(false, "思考中..."));
        uiState.setValue(state.copy().messages(msgs).build());
        saveMessages(msgs);

        List<DeepSeekApi.ChatMessage> messages = new ArrayList<>();
        if (conversationHistory.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
            String currentDate = sdf.format(new Date());
            messages.add(new DeepSeekApi.ChatMessage("system",
                    "当前日期是" + currentDate + "。你是一个专业的A股技术分析助手，回答时请结合当前日期。"));
        }
        messages.addAll(conversationHistory);
        messages.add(new DeepSeekApi.ChatMessage("user", text.trim()));

        DeepSeekApi.chat(messages, new DeepSeekApi.Callback<String>() {
            @Override
            public void onResult(String result) {
                conversationHistory.add(new DeepSeekApi.ChatMessage("user", text.trim()));
                conversationHistory.add(new DeepSeekApi.ChatMessage("assistant", result));
                AiAnalysisState currentState = uiState.getValue();
                if (currentState == null) return;
                List<AiAnalysisState.ChatMessage> msgs = new ArrayList<>(currentState.getMessages());
                if (!msgs.isEmpty()) {
                    AiAnalysisState.ChatMessage last = msgs.get(msgs.size() - 1);
                    if (!last.isUser && "思考中...".equals(last.content)) {
                        msgs.remove(msgs.size() - 1);
                    }
                }
                msgs.add(new AiAnalysisState.ChatMessage(false, result));
                uiState.postValue(currentState.copy().messages(msgs).build());
                saveMessages(msgs);
            }
        });
    }

    public void clearChat() {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        conversationHistory.clear();
        uiState.setValue(state.copy().messages(new ArrayList<>()).build());
        saveMessages(new ArrayList<>());
    }

    private void addMessage(boolean isUser, String content) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        List<AiAnalysisState.ChatMessage> msgs = new ArrayList<>(state.getMessages());
        msgs.add(new AiAnalysisState.ChatMessage(isUser, content));
        uiState.postValue(state.copy().messages(msgs).build());
        saveMessages(msgs);
    }

    private void finishAnalysis() {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return;
        uiState.postValue(state.copy().isAnalyzing(false).build());
        settingsManager.setIsAnalyzing(false);
    }

    private void callDeepSeek(String prompt) {
        if (stopRequested) {
            finishAnalysis();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        String currentDate = sdf.format(new Date());

        conversationHistory = new ArrayList<>();
        conversationHistory.add(new DeepSeekApi.ChatMessage("system",
                "当前日期是" + currentDate + "。你是一个专业稳健的A股技术分析助手。请基于买入价、买入日期和K线数据，进行7维度分析，每个维度不超过30字，总分析不超过200字。直接以编号列出7个维度分析结果，不要额外解释。"));
        conversationHistory.add(new DeepSeekApi.ChatMessage("user", prompt));

        DeepSeekApi.chat(new ArrayList<>(conversationHistory), new DeepSeekApi.Callback<String>() {
            @Override
            public void onResult(String result) {
                conversationHistory.add(new DeepSeekApi.ChatMessage("assistant", result));
                if (!stopRequested) {
                    addMessage(false, result);
                }
                finishAnalysis();
            }
        });
    }

    private String buildAnalysisPrompt(List<KlineData> klineList, List<String> news, List<String> policy) {
        AiAnalysisState state = uiState.getValue();
        if (state == null) return "";

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        String currentDate = sdf.format(new Date());

        sb.append("请基于以下股票的买入价、买入日期和K线数据，进行AI分析并输出7大分析维度（总分析不超过200字）：\n\n");
        sb.append("股票代码: ").append(state.getStockCode()).append("\n");
        sb.append("股票名称: ").append(state.getStockName()).append("\n");
        sb.append("分析日期: ").append(currentDate).append("\n");

        if (state.getBuyPrice() > 0) {
            sb.append("买入价格: ").append(String.format("%.2f", state.getBuyPrice())).append("元\n");
        }
        if (state.getBuyDate() != null && !state.getBuyDate().isEmpty()) {
            sb.append("买入日期: ").append(state.getBuyDate()).append("\n");
        }
        sb.append("止损比例: ").append(String.format("%.1f%%", state.getStopLossPercent())).append("\n");
        sb.append("止盈比例: ").append(String.format("%.1f%%", state.getTargetProfitPercent())).append("\n");

        sb.append("\n=== K线数据摘要 ===\n");
        sb.append("数据周期: 最近 ").append(klineList.size()).append(" 个交易日\n");

        if (!klineList.isEmpty()) {
            KlineData first = klineList.get(0);
            KlineData last = klineList.get(klineList.size() - 1);
            double change = (last.getClose() - first.getClose()) / first.getClose() * 100;
            sb.append(String.format(Locale.CHINA, "区间涨幅: %.2f%%\n", change));
            sb.append(String.format(Locale.CHINA, "起始: %s, 收盘: %.2f\n", first.getDate(), first.getClose()));
            sb.append(String.format(Locale.CHINA, "最新: %s, 收盘: %.2f\n", last.getDate(), last.getClose()));

            double maxHigh = 0, minLow = Double.MAX_VALUE;
            for (KlineData k : klineList) {
                if (k.getHigh() > maxHigh) maxHigh = k.getHigh();
                if (k.getLow() < minLow) minLow = k.getLow();
            }
            sb.append(String.format(Locale.CHINA, "区间最高: %.2f, 区间最低: %.2f\n", maxHigh, minLow));

            sb.append("\n最近10个交易日K线:\n");
            int start = Math.max(0, klineList.size() - 10);
            for (int i = start; i < klineList.size(); i++) {
                KlineData k = klineList.get(i);
                sb.append(String.format(Locale.CHINA, "%s 开:%.2f 收:%.2f 高:%.2f 低:%.2f 涨跌:%.2f%% 量:%.0f\n",
                        k.getDate(), k.getOpen(), k.getClose(), k.getHigh(), k.getLow(), k.getPctChg(), k.getVolume()));
            }

            if (state.getBuyDate() != null && !state.getBuyDate().isEmpty()) {
                for (KlineData k : klineList) {
                    if (k.getDate().equals(state.getBuyDate())) {
                        sb.append(String.format(Locale.CHINA, "\n买入日K线: %s 开:%.2f 收:%.2f 高:%.2f 低:%.2f 涨跌:%.2f%%\n",
                                k.getDate(), k.getOpen(), k.getClose(), k.getHigh(), k.getLow(), k.getPctChg()));
                        break;
                    }
                }
            }
        }

        if (!news.isEmpty()) {
            sb.append("\n=== 相关新闻 ===\n");
            for (String n : news) {
                sb.append("- ").append(n).append("\n");
            }
        }

        if (!policy.isEmpty()) {
            sb.append("\n=== 政策动态 ===\n");
            for (String p : policy) {
                sb.append("- ").append(p).append("\n");
            }
        }

        sb.append("\n请严格按以下7个维度输出分析（每个维度不超过30字，总计不超过200字）：\n");
        sb.append("1. 趋势判断 — 上升/下降/震荡\n");
        sb.append("2. 关键价位 — 支撑位、压力位\n");
        sb.append("3. 技术信号 — 突破/背离/放量\n");
        sb.append("4. 短期预测 — 未来1-5个交易日走势\n");
        sb.append("5. 操作建议 — 买入/持有/减仓/观望\n");
        sb.append("6. 风险提示\n");
        if (state.isUseGraded()) {
            sb.append("建议的分级移动止盈回撤比例: ").append(String.format("%.1f%%", state.getTrailingPercent())).append("\n");
        }
        sb.append("7. 止盈参数建议 — 止损比例、目标止盈比例\n");

        return sb.toString();
    }
}
