package com.hong.xin.stock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hong.xin.stock.data.StrategyManager;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Strategy;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StrategyCheckReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isTradingDay()) return;

        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {
                checkStrategies(context);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    private void checkStrategies(Context context) {
        StrategyManager manager = StrategyManager.getInstance(context);
        List<Strategy> activeStrategies = manager.getActiveStrategies();

        if (activeStrategies.isEmpty()) return;

        java.util.Set<String> stockCodes = new java.util.HashSet<>();
        for (Strategy s : activeStrategies) {
            stockCodes.add(s.getStockCode());
        }

        java.util.Map<String, RealtimeQuote> quoteMap = new java.util.concurrent.ConcurrentHashMap<>();
        final CountDownLatch latch = new CountDownLatch(stockCodes.size());

        for (String code : stockCodes) {
            EastMoneyApi.fetchRealtime(code, quote -> {
                if (quote != null) {
                    quoteMap.put(code, quote);
                }
                latch.countDown();
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (Strategy strategy : activeStrategies) {
            RealtimeQuote quote = quoteMap.get(strategy.getStockCode());
            if (quote == null) continue;

            boolean matched = evaluateStrategy(strategy, quote);
            if (matched) {
                long now = System.currentTimeMillis();
                if (strategy.getLastMatchedAt() > 0
                        && now - strategy.getLastMatchedAt() < 4 * 60 * 60 * 1000) {
                    continue;
                }
                strategy.setLastMatchedAt(now);
                manager.saveStrategy(strategy);
                StrategyNotificationHelper.notifyStrategyMatched(context, strategy);
            }
        }
    }

    private boolean evaluateStrategy(Strategy strategy, RealtimeQuote quote) {
        boolean matched = false;

        double currentPrice = quote.getPrice();

        if (strategy.getConditionPriceAbove() > 0 && currentPrice > strategy.getConditionPriceAbove()) {
            matched = true;
        }
        if (strategy.getConditionPriceBelow() > 0 && currentPrice < strategy.getConditionPriceBelow()) {
            matched = true;
        }

        if (strategy.getConditionPctChangeUp() != 0) {
            double pctChange = quote.getPctChg();
            if (strategy.getConditionPctChangeUp() > 0 && pctChange >= strategy.getConditionPctChangeUp()) {
                matched = true;
            }
            if (strategy.getConditionPctChangeDown() > 0 && pctChange <= -strategy.getConditionPctChangeDown()) {
                matched = true;
            }
        }

        if (strategy.getConditionMaAbove() > 0) {
            double ma = getMaValue(quote, strategy.getConditionMaAbove());
            if (ma > 0 && currentPrice > ma) {
                matched = true;
            }
        }
        if (strategy.getConditionMaBelow() > 0) {
            double ma = getMaValue(quote, strategy.getConditionMaBelow());
            if (ma > 0 && currentPrice < ma) {
                matched = true;
            }
        }

        if (strategy.getConditionVolumeRatioMin() > 0 && quote.getVolumeRatio() >= strategy.getConditionVolumeRatioMin()) {
            matched = true;
        }
        if (strategy.getConditionVolumeRatioMax() > 0 && quote.getVolumeRatio() <= strategy.getConditionVolumeRatioMax()) {
            matched = true;
        }

        if (strategy.getTargetPrice() > 0 && currentPrice >= strategy.getTargetPrice()) {
            matched = true;
        }
        if (strategy.getStopLossPrice() > 0 && currentPrice <= strategy.getStopLossPrice()) {
            matched = true;
        }

        if (strategy.getConditionPriceAbove() <= 0 && strategy.getConditionPriceBelow() <= 0
                && strategy.getConditionPctChangeUp() == 0 && strategy.getConditionPctChangeDown() == 0
                && strategy.getConditionMaAbove() <= 0 && strategy.getConditionMaBelow() <= 0
                && strategy.getConditionVolumeRatioMin() <= 0 && strategy.getConditionVolumeRatioMax() <= 0
                && strategy.getTargetPrice() <= 0 && strategy.getStopLossPrice() <= 0) {
            return false;
        }

        return matched;
    }

    private double getMaValue(RealtimeQuote quote, int maType) {
        switch (maType) {
            case 5: return quote.getMa5();
            case 10: return quote.getMa10();
            case 20: return quote.getMa20();
            case 30: return quote.getMa30();
            case 60: return quote.getMa60();
            default: return 0;
        }
    }

    private boolean isTradingDay() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
    }
}
