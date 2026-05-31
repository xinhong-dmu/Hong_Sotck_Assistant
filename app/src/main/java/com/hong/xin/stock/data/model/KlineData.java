package com.hong.xin.stock.data.model;

import java.util.Objects;

public class KlineData {

    private final String date;
    private final double open;
    private final double close;
    private final double high;
    private final double low;
    private final double volume;
    private final double amount;
    private final double amplitude;
    private final double pctChg;
    private final double change;
    private final double turnover;

    public KlineData(String date, double open, double close, double high, double low,
                     double volume, double amount, double amplitude, double pctChg,
                     double change, double turnover) {
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.amount = amount;
        this.amplitude = amplitude;
        this.pctChg = pctChg;
        this.change = change;
        this.turnover = turnover;
    }

    public String getDate() { return date; }
    public double getOpen() { return open; }
    public double getClose() { return close; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getVolume() { return volume; }
    public double getAmount() { return amount; }
    public double getAmplitude() { return amplitude; }
    public double getPctChg() { return pctChg; }
    public double getChange() { return change; }
    public double getTurnover() { return turnover; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KlineData that = (KlineData) o;
        return Double.compare(that.open, open) == 0 &&
                Double.compare(that.close, close) == 0 &&
                Double.compare(that.high, high) == 0 &&
                Double.compare(that.low, low) == 0 &&
                Double.compare(that.volume, volume) == 0 &&
                Double.compare(that.amount, amount) == 0 &&
                Double.compare(that.amplitude, amplitude) == 0 &&
                Double.compare(that.pctChg, pctChg) == 0 &&
                Double.compare(that.change, change) == 0 &&
                Double.compare(that.turnover, turnover) == 0 &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, open, close, high, low, volume, amount, amplitude, pctChg, change, turnover);
    }

    @Override
    public String toString() {
        return "KlineData{date='" + date + "', open=" + open + ", close=" + close + "}";
    }
}
