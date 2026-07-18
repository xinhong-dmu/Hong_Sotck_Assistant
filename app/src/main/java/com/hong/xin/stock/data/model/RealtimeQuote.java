package com.hong.xin.stock.data.model;

import java.util.Objects;

public class RealtimeQuote {

    private final String name;
    private final String code;
    private final double price;
    private final double high;
    private final double low;
    private final double open;
    private final double volume;
    private final double amount;
    private final double pctChg;
    private final double change;
    private final double preClose;
    private final double pe;
    private final double peTTM;
    private final double pb;
    private final double turnoverRate;
    private final double volumeRatio;
    private final double totalMarketCap;
    private final double circulatingMarketCap;
    private final double limitUp;
    private final double limitDown;
    private final double eps;
    private final double dividendYield;
    private final double ma5;
    private final double ma10;
    private final double ma20;
    private final double ma30;
    private final double ma60;
    private final double iopv;
    private final double premiumRate;

    private RealtimeQuote(Builder builder) {
        this.name = builder.name;
        this.code = builder.code;
        this.price = builder.price;
        this.high = builder.high;
        this.low = builder.low;
        this.open = builder.open;
        this.volume = builder.volume;
        this.amount = builder.amount;
        this.pctChg = builder.pctChg;
        this.change = builder.change;
        this.preClose = builder.preClose;
        this.pe = builder.pe;
        this.peTTM = builder.peTTM;
        this.pb = builder.pb;
        this.turnoverRate = builder.turnoverRate;
        this.volumeRatio = builder.volumeRatio;
        this.totalMarketCap = builder.totalMarketCap;
        this.circulatingMarketCap = builder.circulatingMarketCap;
        this.limitUp = builder.limitUp;
        this.limitDown = builder.limitDown;
        this.eps = builder.eps;
        this.dividendYield = builder.dividendYield;
        this.ma5 = builder.ma5;
        this.ma10 = builder.ma10;
        this.ma20 = builder.ma20;
        this.ma30 = builder.ma30;
        this.ma60 = builder.ma60;
        this.iopv = builder.iopv;
        this.premiumRate = builder.premiumRate;
    }

    public RealtimeQuote() {
        this.name = "";
        this.code = "";
        this.price = 0.0;
        this.high = 0.0;
        this.low = 0.0;
        this.open = 0.0;
        this.volume = 0.0;
        this.amount = 0.0;
        this.pctChg = 0.0;
        this.change = 0.0;
        this.preClose = 0.0;
        this.pe = 0.0;
        this.peTTM = 0.0;
        this.pb = 0.0;
        this.turnoverRate = 0.0;
        this.volumeRatio = 0.0;
        this.totalMarketCap = 0.0;
        this.circulatingMarketCap = 0.0;
        this.limitUp = 0.0;
        this.limitDown = 0.0;
        this.eps = 0.0;
        this.dividendYield = 0.0;
        this.ma5 = 0.0;
        this.ma10 = 0.0;
        this.ma20 = 0.0;
        this.ma30 = 0.0;
        this.ma60 = 0.0;
        this.iopv = 0.0;
        this.premiumRate = 0.0;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public double getPrice() { return price; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getOpen() { return open; }
    public double getVolume() { return volume; }
    public double getAmount() { return amount; }
    public double getPctChg() { return pctChg; }
    public double getChange() { return change; }
    public double getPreClose() { return preClose; }
    public double getPe() { return pe; }
    public double getPeTTM() { return peTTM; }
    public double getPb() { return pb; }
    public double getTurnoverRate() { return turnoverRate; }
    public double getVolumeRatio() { return volumeRatio; }
    public double getTotalMarketCap() { return totalMarketCap; }
    public double getCirculatingMarketCap() { return circulatingMarketCap; }
    public double getLimitUp() { return limitUp; }
    public double getLimitDown() { return limitDown; }
    public double getEps() { return eps; }
    public double getDividendYield() { return dividendYield; }
    public double getMa5() { return ma5; }
    public double getMa10() { return ma10; }
    public double getMa20() { return ma20; }
    public double getMa30() { return ma30; }
    public double getMa60() { return ma60; }
    public double getIopv() { return iopv; }
    public double getPremiumRate() { return premiumRate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RealtimeQuote that = (RealtimeQuote) o;
        return Double.compare(that.price, price) == 0 &&
                Double.compare(that.high, high) == 0 &&
                Double.compare(that.low, low) == 0 &&
                Double.compare(that.open, open) == 0 &&
                Double.compare(that.volume, volume) == 0 &&
                Double.compare(that.amount, amount) == 0 &&
                Double.compare(that.pctChg, pctChg) == 0 &&
                Double.compare(that.change, change) == 0 &&
                Double.compare(that.preClose, preClose) == 0 &&
                Double.compare(that.pe, pe) == 0 &&
                Double.compare(that.peTTM, peTTM) == 0 &&
                Double.compare(that.pb, pb) == 0 &&
                Double.compare(that.turnoverRate, turnoverRate) == 0 &&
                Double.compare(that.volumeRatio, volumeRatio) == 0 &&
                Double.compare(that.totalMarketCap, totalMarketCap) == 0 &&
                Double.compare(that.circulatingMarketCap, circulatingMarketCap) == 0 &&
                Double.compare(that.limitUp, limitUp) == 0 &&
                Double.compare(that.limitDown, limitDown) == 0 &&
                Double.compare(that.eps, eps) == 0 &&
                Double.compare(that.dividendYield, dividendYield) == 0 &&
                Double.compare(that.ma5, ma5) == 0 &&
                Double.compare(that.ma10, ma10) == 0 &&
                Double.compare(that.ma20, ma20) == 0 &&
                Double.compare(that.ma30, ma30) == 0 &&
                Double.compare(that.ma60, ma60) == 0 &&
                Objects.equals(name, that.name) &&
                Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code, price, high, low, open, volume, amount,
                pctChg, change, preClose, pe, peTTM, pb, turnoverRate, volumeRatio,
                totalMarketCap, circulatingMarketCap, limitUp, limitDown, eps,
                dividendYield, ma5, ma10, ma20, ma30, ma60);
    }

    @Override
    public String toString() {
        return "RealtimeQuote{name='" + name + "', code='" + code +
                "', price=" + price + ", pctChg=" + pctChg +
                ", volumeRatio=" + volumeRatio + ", turnoverRate=" + turnoverRate + "}";
    }

    public static class Builder {
        private String name = "";
        private String code = "";
        private double price = 0.0;
        private double high = 0.0;
        private double low = 0.0;
        private double open = 0.0;
        private double volume = 0.0;
        private double amount = 0.0;
        private double pctChg = 0.0;
        private double change = 0.0;
        private double preClose = 0.0;
        private double pe = 0.0;
        private double peTTM = 0.0;
        private double pb = 0.0;
        private double turnoverRate = 0.0;
        private double volumeRatio = 0.0;
        private double totalMarketCap = 0.0;
        private double circulatingMarketCap = 0.0;
        private double limitUp = 0.0;
        private double limitDown = 0.0;
        private double eps = 0.0;
        private double dividendYield = 0.0;
        private double ma5 = 0.0;
        private double ma10 = 0.0;
        private double ma20 = 0.0;
        private double ma30 = 0.0;
        private double ma60 = 0.0;
        private double iopv = 0.0;
        private double premiumRate = 0.0;

        public Builder name(String name) { this.name = name; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder price(double price) { this.price = price; return this; }
        public Builder high(double high) { this.high = high; return this; }
        public Builder low(double low) { this.low = low; return this; }
        public Builder open(double open) { this.open = open; return this; }
        public Builder volume(double volume) { this.volume = volume; return this; }
        public Builder amount(double amount) { this.amount = amount; return this; }
        public Builder pctChg(double pctChg) { this.pctChg = pctChg; return this; }
        public Builder change(double change) { this.change = change; return this; }
        public Builder preClose(double preClose) { this.preClose = preClose; return this; }
        public Builder pe(double pe) { this.pe = pe; return this; }
        public Builder peTTM(double peTTM) { this.peTTM = peTTM; return this; }
        public Builder pb(double pb) { this.pb = pb; return this; }
        public Builder turnoverRate(double turnoverRate) { this.turnoverRate = turnoverRate; return this; }
        public Builder volumeRatio(double volumeRatio) { this.volumeRatio = volumeRatio; return this; }
        public Builder totalMarketCap(double totalMarketCap) { this.totalMarketCap = totalMarketCap; return this; }
        public Builder circulatingMarketCap(double circulatingMarketCap) { this.circulatingMarketCap = circulatingMarketCap; return this; }
        public Builder limitUp(double limitUp) { this.limitUp = limitUp; return this; }
        public Builder limitDown(double limitDown) { this.limitDown = limitDown; return this; }
        public Builder eps(double eps) { this.eps = eps; return this; }
        public Builder dividendYield(double dividendYield) { this.dividendYield = dividendYield; return this; }
        public Builder ma5(double ma5) { this.ma5 = ma5; return this; }
        public Builder ma10(double ma10) { this.ma10 = ma10; return this; }
        public Builder ma20(double ma20) { this.ma20 = ma20; return this; }
        public Builder ma30(double ma30) { this.ma30 = ma30; return this; }
        public Builder ma60(double ma60) { this.ma60 = ma60; return this; }
        public Builder iopv(double iopv) { this.iopv = iopv; return this; }
        public Builder premiumRate(double premiumRate) { this.premiumRate = premiumRate; return this; }

        public RealtimeQuote build() {
            return new RealtimeQuote(this);
        }
    }
}
