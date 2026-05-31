package com.hong.xin.stock.data.model;

import java.util.Objects;

public class Stock {

    private final String code;
    private final String name;

    public Stock(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(code, stock.code) && Objects.equals(name, stock.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name);
    }

    @Override
    public String toString() {
        return "Stock{code='" + code + "', name='" + name + "'}";
    }
}
