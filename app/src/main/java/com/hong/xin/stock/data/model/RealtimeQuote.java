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
                Objects.equals(name, that.name) &&
                Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code, price, high, low, open, volume, amount, pctChg, change, preClose, pe);
    }

    @Override
    public String toString() {
        return "RealtimeQuote{name='" + name + "', code='" + code + "', price=" + price + "}";
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

        public RealtimeQuote build() {
            return new RealtimeQuote(this);
        }
    }
}
