package com.hong.xin.stock.ui.trade;

public class TradeUiState {

    private final String stockName;
    private final String stockCode;
    private final double buyPrice;
    private final String buyDate;
    private final double stopLossPercent;
    private final double targetProfitPercent;
    private final double trailingPercent;
    private final boolean useGraded;
    private final double currentPrice;

    private final boolean isActive;
    private final double highestPrice;
    private final double defenseLine;
    private final double hardStopLine;
    private final double profitPct;
    private final double drawdown;
    private final double distanceToDefense;
    private final double effectiveTrailing;
    private final boolean isGradedEffect;

    private final String lastMessage;
    private final boolean dashboardVisible;

    private TradeUiState(Builder builder) {
        this.stockName = builder.stockName;
        this.stockCode = builder.stockCode;
        this.buyPrice = builder.buyPrice;
        this.buyDate = builder.buyDate;
        this.stopLossPercent = builder.stopLossPercent;
        this.targetProfitPercent = builder.targetProfitPercent;
        this.trailingPercent = builder.trailingPercent;
        this.useGraded = builder.useGraded;
        this.currentPrice = builder.currentPrice;
        this.isActive = builder.isActive;
        this.highestPrice = builder.highestPrice;
        this.defenseLine = builder.defenseLine;
        this.hardStopLine = builder.hardStopLine;
        this.profitPct = builder.profitPct;
        this.drawdown = builder.drawdown;
        this.distanceToDefense = builder.distanceToDefense;
        this.effectiveTrailing = builder.effectiveTrailing;
        this.isGradedEffect = builder.isGradedEffect;
        this.lastMessage = builder.lastMessage;
        this.dashboardVisible = builder.dashboardVisible;
    }

    public String getStockName() { return stockName; }
    public String getStockCode() { return stockCode; }
    public double getBuyPrice() { return buyPrice; }
    public String getBuyDate() { return buyDate; }
    public double getStopLossPercent() { return stopLossPercent; }
    public double getTargetProfitPercent() { return targetProfitPercent; }
    public double getTrailingPercent() { return trailingPercent; }
    public boolean isUseGraded() { return useGraded; }
    public double getCurrentPrice() { return currentPrice; }
    public boolean isActive() { return isActive; }
    public double getHighestPrice() { return highestPrice; }
    public double getDefenseLine() { return defenseLine; }
    public double getHardStopLine() { return hardStopLine; }
    public double getProfitPct() { return profitPct; }
    public double getDrawdown() { return drawdown; }
    public double getDistanceToDefense() { return distanceToDefense; }
    public double getEffectiveTrailing() { return effectiveTrailing; }
    public boolean isGradedEffect() { return isGradedEffect; }
    public String getLastMessage() { return lastMessage; }
    public boolean isDashboardVisible() { return dashboardVisible; }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder()
                .stockName(stockName).stockCode(stockCode).buyPrice(buyPrice).buyDate(buyDate)
                .stopLossPercent(stopLossPercent).targetProfitPercent(targetProfitPercent)
                .trailingPercent(trailingPercent).useGraded(useGraded).currentPrice(currentPrice)
                .isActive(isActive).highestPrice(highestPrice).defenseLine(defenseLine)
                .hardStopLine(hardStopLine).profitPct(profitPct).drawdown(drawdown)
                .distanceToDefense(distanceToDefense).effectiveTrailing(effectiveTrailing)
                .isGradedEffect(isGradedEffect).lastMessage(lastMessage).dashboardVisible(dashboardVisible);
    }

    public static class Builder {
        private String stockName = "";
        private String stockCode = "";
        private double buyPrice = 0;
        private String buyDate = "";
        private double stopLossPercent = 3;
        private double targetProfitPercent = 10;
        private double trailingPercent = 0;
        private boolean useGraded = false;
        private double currentPrice = 0;
        private boolean isActive = false;
        private double highestPrice = 0;
        private double defenseLine = 0;
        private double hardStopLine = 0;
        private double profitPct = 0;
        private double drawdown = 0;
        private double distanceToDefense = 0;
        private double effectiveTrailing = 0;
        private boolean isGradedEffect = false;
        private String lastMessage = "";
        private boolean dashboardVisible = false;

        public Builder stockName(String v) { this.stockName = v; return this; }
        public Builder stockCode(String v) { this.stockCode = v; return this; }
        public Builder buyPrice(double v) { this.buyPrice = v; return this; }
        public Builder buyDate(String v) { this.buyDate = v; return this; }
        public Builder stopLossPercent(double v) { this.stopLossPercent = v; return this; }
        public Builder targetProfitPercent(double v) { this.targetProfitPercent = v; return this; }
        public Builder trailingPercent(double v) { this.trailingPercent = v; return this; }
        public Builder useGraded(boolean v) { this.useGraded = v; return this; }
        public Builder currentPrice(double v) { this.currentPrice = v; return this; }
        public Builder isActive(boolean v) { this.isActive = v; return this; }
        public Builder highestPrice(double v) { this.highestPrice = v; return this; }
        public Builder defenseLine(double v) { this.defenseLine = v; return this; }
        public Builder hardStopLine(double v) { this.hardStopLine = v; return this; }
        public Builder profitPct(double v) { this.profitPct = v; return this; }
        public Builder drawdown(double v) { this.drawdown = v; return this; }
        public Builder distanceToDefense(double v) { this.distanceToDefense = v; return this; }
        public Builder effectiveTrailing(double v) { this.effectiveTrailing = v; return this; }
        public Builder isGradedEffect(boolean v) { this.isGradedEffect = v; return this; }
        public Builder lastMessage(String v) { this.lastMessage = v; return this; }
        public Builder dashboardVisible(boolean v) { this.dashboardVisible = v; return this; }

        public TradeUiState build() { return new TradeUiState(this); }
    }
}
