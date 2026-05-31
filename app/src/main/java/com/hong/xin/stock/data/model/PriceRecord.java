package com.hong.xin.stock.data.model;

import java.util.Objects;

public class PriceRecord {

    private final String time;
    private final String stockCode;
    private final String stockName;
    private final double price;
    private final double buyPrice;
    private final double highestPrice;
    private final double defenseLine;
    private final double hardStopLine;
    private final double profitPct;
    private final double drawdown;
    private final String action;

    public PriceRecord(String time, String stockCode, String stockName,
                       double price, double buyPrice, double highestPrice,
                       double defenseLine, double hardStopLine,
                       double profitPct, double drawdown, String action) {
        this.time = time;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.price = price;
        this.buyPrice = buyPrice;
        this.highestPrice = highestPrice;
        this.defenseLine = defenseLine;
        this.hardStopLine = hardStopLine;
        this.profitPct = profitPct;
        this.drawdown = drawdown;
        this.action = action;
    }

    public String getTime() { return time; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public double getPrice() { return price; }
    public double getBuyPrice() { return buyPrice; }
    public double getHighestPrice() { return highestPrice; }
    public double getDefenseLine() { return defenseLine; }
    public double getHardStopLine() { return hardStopLine; }
    public double getProfitPct() { return profitPct; }
    public double getDrawdown() { return drawdown; }
    public String getAction() { return action; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceRecord that = (PriceRecord) o;
        return Double.compare(that.price, price) == 0 &&
                Double.compare(that.buyPrice, buyPrice) == 0 &&
                Double.compare(that.highestPrice, highestPrice) == 0 &&
                Double.compare(that.defenseLine, defenseLine) == 0 &&
                Double.compare(that.hardStopLine, hardStopLine) == 0 &&
                Double.compare(that.profitPct, profitPct) == 0 &&
                Double.compare(that.drawdown, drawdown) == 0 &&
                Objects.equals(time, that.time) &&
                Objects.equals(stockCode, that.stockCode) &&
                Objects.equals(stockName, that.stockName) &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, stockCode, stockName, price, buyPrice, highestPrice,
                defenseLine, hardStopLine, profitPct, drawdown, action);
    }

    @Override
    public String toString() {
        return "PriceRecord{time='" + time + "', stock='" + stockCode + "', price=" + price +
                ", profit=" + String.format("%.2f", profitPct) + "%, action='" + action + "'}";
    }
}
