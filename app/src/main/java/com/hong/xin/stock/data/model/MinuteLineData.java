package com.hong.xin.stock.data.model;

import java.util.Objects;

public class MinuteLineData {

    private final String time;
    private final double price;
    private final double avgPrice;
    private final double volume;
    private final double amount;
    private final double preClose;

    public MinuteLineData(String time, double price, double avgPrice,
                          double volume, double amount, double preClose) {
        this.time = time;
        this.price = price;
        this.avgPrice = avgPrice;
        this.volume = volume;
        this.amount = amount;
        this.preClose = preClose;
    }

    public String getTime() { return time; }
    public double getPrice() { return price; }
    public double getAvgPrice() { return avgPrice; }
    public double getVolume() { return volume; }
    public double getAmount() { return amount; }
    public double getPreClose() { return preClose; }

    public double getPctChg() {
        return preClose > 0 ? (price - preClose) / preClose * 100.0 : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinuteLineData that = (MinuteLineData) o;
        return Double.compare(that.price, price) == 0 &&
                Double.compare(that.avgPrice, avgPrice) == 0 &&
                Double.compare(that.volume, volume) == 0 &&
                Double.compare(that.amount, amount) == 0 &&
                Double.compare(that.preClose, preClose) == 0 &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, price, avgPrice, volume, amount, preClose);
    }

    @Override
    public String toString() {
        return "MinuteLineData{time='" + time + "', price=" + price +
                ", volume=" + volume + ", avgPrice=" + avgPrice + "}";
    }
}
