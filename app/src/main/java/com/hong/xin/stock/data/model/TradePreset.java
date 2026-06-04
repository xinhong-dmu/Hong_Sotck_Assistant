package com.hong.xin.stock.data.model;

public class TradePreset {

    private String id;
    private String stockName;
    private String stockCode;
    private double buyPrice;
    private String buyDate;
    private double stopLossPercent;
    private double targetProfitPercent;
    private double trailingPercent;
    private boolean useGraded;
    private String savedAt;

    public TradePreset() {}

    public TradePreset(String id, String stockName, String stockCode, double buyPrice, String buyDate,
                       double stopLossPercent, double targetProfitPercent, double trailingPercent,
                       boolean useGraded, String savedAt) {
        this.id = id;
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.buyPrice = buyPrice;
        this.buyDate = buyDate;
        this.stopLossPercent = stopLossPercent;
        this.targetProfitPercent = targetProfitPercent;
        this.trailingPercent = trailingPercent;
        this.useGraded = useGraded;
        this.savedAt = savedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public String getBuyDate() { return buyDate; }
    public void setBuyDate(String buyDate) { this.buyDate = buyDate; }

    public double getStopLossPercent() { return stopLossPercent; }
    public void setStopLossPercent(double stopLossPercent) { this.stopLossPercent = stopLossPercent; }

    public double getTargetProfitPercent() { return targetProfitPercent; }
    public void setTargetProfitPercent(double targetProfitPercent) { this.targetProfitPercent = targetProfitPercent; }

    public double getTrailingPercent() { return trailingPercent; }
    public void setTrailingPercent(double trailingPercent) { this.trailingPercent = trailingPercent; }

    public boolean isUseGraded() { return useGraded; }
    public void setUseGraded(boolean useGraded) { this.useGraded = useGraded; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public String getDisplayTitle() {
        if (stockName != null && !stockName.isEmpty()) {
            return stockName + " (" + stockCode + ")";
        }
        return stockCode;
    }

    public String getDisplaySubtitle() {
        StringBuilder sb = new StringBuilder();
        sb.append("买入价: ").append(String.format("%.2f", buyPrice));
        if (buyDate != null && !buyDate.isEmpty()) {
            sb.append(" | 日期: ").append(buyDate);
        }
        return sb.toString();
    }

    public String getDisplayParams() {
        StringBuilder sb = new StringBuilder();
        sb.append("止损: ").append(String.format("%.0f%%", stopLossPercent));
        sb.append(" | 止盈: ").append(String.format("%.0f%%", targetProfitPercent));
        if (useGraded) {
            sb.append(" | 回撤: ").append(String.format("%.0f%%", trailingPercent));
            sb.append(" | 分级: 开");
        }
        return sb.toString();
    }
}
