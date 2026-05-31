package com.hong.xin.stock;

import android.app.Application;

import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.util.DebugLogger;

public class StockApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DebugLogger.init(this);
        StockRepository.getInstance().init(this);
    }
}
