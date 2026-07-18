package com.hong.xin.stock.data.model;

public class PurchaseRecord {

    private String stockCode;
    private double price;
    private String date;

    public PurchaseRecord() {}

    public PurchaseRecord(String stockCode, double price, String date) {
        this.stockCode = stockCode;
        this.price = price;
        this.date = date;
    }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
