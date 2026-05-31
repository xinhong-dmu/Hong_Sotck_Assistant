package com.hong.xin.stock.ui.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiAnalysisState {

    private final String stockCode;
    private final String stockName;
    private final double buyPrice;
    private final String buyDate;
    private final int klineDays;
    private final double stopLossPercent;
    private final double targetProfitPercent;
    private final double trailingPercent;
    private final boolean useGraded;
    private final boolean isAnalyzing;
    private final List<ChatMessage> messages;

    private AiAnalysisState(Builder builder) {
        this.stockCode = builder.stockCode;
        this.stockName = builder.stockName;
        this.buyPrice = builder.buyPrice;
        this.buyDate = builder.buyDate;
        this.klineDays = builder.klineDays;
        this.stopLossPercent = builder.stopLossPercent;
        this.targetProfitPercent = builder.targetProfitPercent;
        this.trailingPercent = builder.trailingPercent;
        this.useGraded = builder.useGraded;
        this.isAnalyzing = builder.isAnalyzing;
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
    }

    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public double getBuyPrice() { return buyPrice; }
    public String getBuyDate() { return buyDate; }
    public int getKlineDays() { return klineDays; }
    public double getStopLossPercent() { return stopLossPercent; }
    public double getTargetProfitPercent() { return targetProfitPercent; }
    public double getTrailingPercent() { return trailingPercent; }
    public boolean isUseGraded() { return useGraded; }
    public boolean isAnalyzing() { return isAnalyzing; }
    public List<ChatMessage> getMessages() { return messages; }

    public static Builder builder() { return new Builder(); }

    public Builder copy() {
        return new Builder()
                .stockCode(stockCode).stockName(stockName)
                .buyPrice(buyPrice).buyDate(buyDate).klineDays(klineDays)
                .stopLossPercent(stopLossPercent).targetProfitPercent(targetProfitPercent)
                .trailingPercent(trailingPercent).useGraded(useGraded)
                .isAnalyzing(isAnalyzing).messages(messages);
    }

    public static class ChatMessage {
        public boolean isUser;
        public String content;

        ChatMessage() {} // Gson

        public ChatMessage(boolean isUser, String content) {
            this.isUser = isUser;
            this.content = content;
        }
    }

    public static class Builder {
        private String stockCode = "";
        private String stockName = "";
        private double buyPrice = 0;
        private String buyDate = "";
        private int klineDays = 120;
        private double stopLossPercent = 3;
        private double targetProfitPercent = 10;
        private double trailingPercent = 0;
        private boolean useGraded = false;
        private boolean isAnalyzing = false;
        private List<ChatMessage> messages = new ArrayList<>();

        public Builder stockCode(String v) { this.stockCode = v; return this; }
        public Builder stockName(String v) { this.stockName = v; return this; }
        public Builder buyPrice(double v) { this.buyPrice = v; return this; }
        public Builder buyDate(String v) { this.buyDate = v; return this; }
        public Builder klineDays(int v) { this.klineDays = v; return this; }
        public Builder stopLossPercent(double v) { this.stopLossPercent = v; return this; }
        public Builder targetProfitPercent(double v) { this.targetProfitPercent = v; return this; }
        public Builder trailingPercent(double v) { this.trailingPercent = v; return this; }
        public Builder useGraded(boolean v) { this.useGraded = v; return this; }
        public Builder isAnalyzing(boolean v) { this.isAnalyzing = v; return this; }
        public Builder messages(List<ChatMessage> v) { this.messages = new ArrayList<>(v); return this; }

        public AiAnalysisState build() { return new AiAnalysisState(this); }
    }
}
