package com.hong.xin.stock.ui.search;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.api.StockNewsApi;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockSearchViewModel extends AndroidViewModel {

    private final MutableLiveData<StockSearchUiState> uiState = new MutableLiveData<>(StockSearchUiState.builder().build());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Stock>> searchResults = new MutableLiveData<>();
    private final StockRepository stockRepository;
    private final SettingsManager settingsManager;
    private boolean stopRequested = false;
    private List<DeepSeekApi.ChatMessage> conversationHistory = new ArrayList<>();

    public StockSearchViewModel(Application application) {
        super(application);
        this.stockRepository = StockRepository.getInstance();
        this.settingsManager = new SettingsManager(application);
        String savedKey = settingsManager.getApiKey();
        if (!savedKey.isEmpty()) {
            DeepSeekApi.setApiKey(savedKey);
        }
        String savedModel = settingsManager.getDeepSeekModel();
        if (!savedModel.isEmpty()) {
            DeepSeekApi.setModel(savedModel);
        }
    }

    public LiveData<StockSearchUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<List<Stock>> getSearchResults() {
        return searchResults;
    }

    public void searchStock(String keyword) {
        searchResults.setValue(stockRepository.searchAll(keyword));
    }

    public void selectStock(Stock stock) {
        StockSearchUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy().selectedStock(stock).build());
    }

    public void startAiAnalysis() {
        StockSearchUiState state = uiState.getValue();
        if (state == null || state.getSelectedStock() == null) {
            toastMessage.setValue("请先搜索并选择一只股票");
            return;
        }

        String apiKey = settingsManager.getApiKey();
        if (apiKey.isEmpty()) {
            toastMessage.setValue("请先在AI分析页面设置DeepSeek API Key");
            return;
        }

        stopRequested = false;
        DeepSeekApi.setApiKey(apiKey);
        uiState.setValue(state.copy().isAnalyzing(true).build());

        Stock stock = state.getSelectedStock();
        addMessage(false, "正在分析 " + stock.getName() + "(" + stock.getCode() + ")...");
        addMessage(false, "正在获取K线数据和新闻...");

        EastMoneyApi.fetchKline(stock.getCode(), 120, new EastMoneyApi.Callback<List<KlineData>>() {
            @Override
            public void onResult(List<KlineData> klineList) {
                if (stopRequested) {
                    finishAnalysis();
                    return;
                }

                if (klineList.isEmpty()) {
                    addMessage(false, "获取K线数据失败，请检查股票代码");
                    finishAnalysis();
                    return;
                }

                new Thread(() -> {
                    List<String> news = StockNewsApi.getStockNewsSync(stock.getCode(), stock.getName(), 5);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (stopRequested) {
                            finishAnalysis();
                            return;
                        }

                        addMessage(false, "正在调用DeepSeek AI生成分析报告...");
                        String prompt = buildSimpleAnalysisPrompt(stock, klineList, news);
                        callDeepSeek(prompt);
                    });
                }).start();
            }
        });
    }

    public void stopAnalysis() {
        stopRequested = true;
        StockSearchUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy().isAnalyzing(false).build());
        addMessage(false, "分析已停止");
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        StockSearchUiState state = uiState.getValue();
        if (state == null) return;

        String apiKey = settingsManager.getApiKey();
        if (apiKey.isEmpty()) {
            List<StockSearchUiState.ChatMessage> msgs = new ArrayList<>(state.getMessages());
            msgs.add(new StockSearchUiState.ChatMessage(true, text.trim()));
            msgs.add(new StockSearchUiState.ChatMessage(false, "请先在AI分析页面设置DeepSeek API Key"));
            uiState.setValue(state.copy().messages(msgs).build());
            return;
        }
        DeepSeekApi.setApiKey(apiKey);

        List<StockSearchUiState.ChatMessage> msgs = new ArrayList<>(state.getMessages());
        msgs.add(new StockSearchUiState.ChatMessage(true, text.trim()));
        msgs.add(new StockSearchUiState.ChatMessage(false, "思考中..."));
        uiState.setValue(state.copy().messages(msgs).build());

        List<DeepSeekApi.ChatMessage> messages = new ArrayList<>(conversationHistory);
        messages.add(new DeepSeekApi.ChatMessage("user", text.trim()));

        DeepSeekApi.chat(messages, new DeepSeekApi.Callback<String>() {
            @Override
            public void onResult(String result) {
                conversationHistory.add(new DeepSeekApi.ChatMessage("user", text.trim()));
                conversationHistory.add(new DeepSeekApi.ChatMessage("assistant", result));
                StockSearchUiState currentState = uiState.getValue();
                if (currentState == null) return;
                List<StockSearchUiState.ChatMessage> msgs = new ArrayList<>(currentState.getMessages());
                if (!msgs.isEmpty()) {
                    StockSearchUiState.ChatMessage last = msgs.get(msgs.size() - 1);
                    if (!last.isUser && "思考中...".equals(last.content)) {
                        msgs.remove(msgs.size() - 1);
                    }
                }
                msgs.add(new StockSearchUiState.ChatMessage(false, result));
                uiState.postValue(currentState.copy().messages(msgs).build());
            }
        });
    }

    public void clearChat() {
        StockSearchUiState state = uiState.getValue();
        if (state == null) return;
        conversationHistory.clear();
        state.copy().messages(new ArrayList<>()).build();
        uiState.setValue(state.copy().messages(new ArrayList<>()).build());
    }

    private void addMessage(boolean isUser, String content) {
        StockSearchUiState state = uiState.getValue();
        if (state == null) return;
        List<StockSearchUiState.ChatMessage> msgs = new ArrayList<>(state.getMessages());
        msgs.add(new StockSearchUiState.ChatMessage(isUser, content));
        uiState.postValue(state.copy().messages(msgs).build());
    }

    private void finishAnalysis() {
        StockSearchUiState state = uiState.getValue();
        if (state == null) return;
        uiState.postValue(state.copy().isAnalyzing(false).build());
    }

    private void callDeepSeek(String prompt) {
        if (stopRequested) {
            finishAnalysis();
            return;
        }

        conversationHistory = new ArrayList<>();
        conversationHistory.add(new DeepSeekApi.ChatMessage("system",
                "你是一个专业的A股股票分析助手。请基于提供的K线数据和新闻，给出简洁实用的投资分析。"));
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

    private String buildSimpleAnalysisPrompt(Stock stock, List<KlineData> klineList, List<String> news) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下A股股票：\n\n");
        sb.append("代码: ").append(stock.getCode()).append("\n");
        sb.append("名称: ").append(stock.getName()).append("\n\n");

        sb.append("=== K线数据（最近").append(klineList.size()).append("日）===\n");
        if (!klineList.isEmpty()) {
            KlineData first = klineList.get(0);
            KlineData last = klineList.get(klineList.size() - 1);
            double change = (last.getClose() - first.getClose()) / first.getClose() * 100;
            sb.append(String.format(Locale.CHINA, "区间涨幅: %.2f%%\n", change));
            sb.append(String.format(Locale.CHINA, "最新收盘: %.2f (%s)\n", last.getClose(), last.getDate()));
        }

        if (!news.isEmpty()) {
            sb.append("\n=== 相关新闻 ===\n");
            for (String n : news) {
                sb.append("- ").append(n).append("\n");
            }
        }

        sb.append("\n请给出简要的技术分析和投资建议。");
        return sb.toString();
    }
}
