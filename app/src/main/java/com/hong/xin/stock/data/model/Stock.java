package com.hong.xin.stock.data.model;

import java.util.Objects;

public class Stock {

    public static final String TYPE_STOCK = "stock";
    public static final String TYPE_ETF = "etf";

    private String code;
    private String name;
    private String type;

    public Stock() {}

    public Stock(String code, String name) {
        this(code, name, TYPE_STOCK);
    }

    public Stock(String code, String name, String type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isEtf() { return TYPE_ETF.equals(type); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(code, stock.code) && Objects.equals(name, stock.name) && Objects.equals(type, stock.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, type);
    }

    @Override
    public String toString() {
        return "Stock{code='" + code + "', name='" + name + "', type='" + type + "'}";
    }
}
