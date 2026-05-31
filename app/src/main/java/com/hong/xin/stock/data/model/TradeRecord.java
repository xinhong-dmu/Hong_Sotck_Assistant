package com.hong.xin.stock.data.model;

import java.util.Objects;

public class TradeRecord {

    private final String time;
    private final String stockName;
    private final String stockCode;
    private final double buyPrice;
    private final double highestPrice;
    private final double exitPrice;
    private final double profitPct;
    private final String reason;

    public TradeRecord(String time, String stockName, String stockCode, double buyPrice,
                       double highestPrice, double exitPrice, double profitPct, String reason) {
        this.time = time;
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.buyPrice = buyPrice;
        this.highestPrice = highestPrice;
        this.exitPrice = exitPrice;
        this.profitPct = profitPct;
        this.reason = reason;
    }

    public String getTime() { return time; }
    public String getStockName() { return stockName; }
    public String getStockCode() { return stockCode; }
    public double getBuyPrice() { return buyPrice; }
    public double getHighestPrice() { return highestPrice; }
    public double getExitPrice() { return exitPrice; }
    public double getProfitPct() { return profitPct; }
    public String getReason() { return reason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeRecord that = (TradeRecord) o;
        return Double.compare(that.buyPrice, buyPrice) == 0 &&
                Double.compare(that.highestPrice, highestPrice) == 0 &&
                Double.compare(that.exitPrice, exitPrice) == 0 &&
                Double.compare(that.profitPct, profitPct) == 0 &&
                Objects.equals(time, that.time) &&
                Objects.equals(stockName, that.stockName) &&
                Objects.equals(stockCode, that.stockCode) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, stockName, stockCode, buyPrice, highestPrice, exitPrice, profitPct, reason);
    }

    @Override
    public String toString() {
        return "TradeRecord{time='" + time + "', stockName='" + stockName + "', profitPct=" + profitPct + "}";
    }
}
