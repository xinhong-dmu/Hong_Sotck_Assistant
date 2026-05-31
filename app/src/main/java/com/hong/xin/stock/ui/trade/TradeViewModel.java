package com.hong.xin.stock.ui.trade;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.PriceRecord;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.repository.PriceHistoryRepository;
import com.hong.xin.stock.data.repository.TradeRepository;
import com.hong.xin.stock.domain.TrailingStopEngine;
import com.hong.xin.stock.util.DebugLogger;
import com.hong.xin.stock.util.SettingsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TradeViewModel extends AndroidViewModel {

    private final MutableLiveData<TradeUiState> uiState = new MutableLiveData<>(TradeUiState.builder().build());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> alertDialog = new MutableLiveData<>();

    private TrailingStopEngine engine;
    private final SettingsManager settingsManager;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public TradeViewModel(Application application) {
        super(application);
        this.settingsManager = new SettingsManager(application);
        restoreState();
    }

    public LiveData<TradeUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<String> getAlertDialog() {
        return alertDialog;
    }

    public void clearAlertDialog() {
        alertDialog.setValue(null);
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    private void restoreState() {
        TradeUiState state = uiState.getValue();
        if (state == null) return;
        TradeUiState.Builder builder = state.copy()
                .stockName(settingsManager.getLastStockName())
                .stockCode(settingsManager.getLastStockCode())
                .buyPrice(settingsManager.getLastBuyPrice())
                .buyDate(settingsManager.getLastBuyDate())
                .stopLossPercent(settingsManager.getStopLossPercent())
                .targetProfitPercent(settingsManager.getTargetProfitPercent())
                .trailingPercent(settingsManager.getTrailingPercent())
                .useGraded(settingsManager.getUseGraded())
                .currentPrice(settingsManager.getCurrentPrice())
                .dashboardVisible(settingsManager.isDashboardVisible())
                .highestPrice(0)
                .defenseLine(settingsManager.getDefenseLine())
                .hardStopLine(settingsManager.getHardStopLine())
                .isActive(settingsManager.getIsActive());

        uiState.setValue(builder.build());

        if (settingsManager.isDashboardVisible() && settingsManager.getIsActive()) {
            String buyDate = settingsManager.getLastBuyDate();
            if (buyDate != null && !buyDate.isEmpty()) {
                restoreEngineFromKline(buyDate);
            } else {
                TradeUiState s = uiState.getValue();
                if (s != null) {
                    TradeUiState.Builder b = s.copy();
                    recalcDashboardFields(b, settingsManager.getCurrentPrice());
                    uiState.setValue(b.build());
                }
            }
        }
    }

    private void restoreEngineFromKline(String buyDate) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        EastMoneyApi.fetchKline(state.getStockCode(), 365, new EastMoneyApi.Callback<List<KlineData>>() {
            @Override
            public void onResult(List<KlineData> klineList) {
                TradeUiState s = uiState.getValue();
                if (s == null) return;

                double initialHighest = s.getBuyPrice();
                boolean foundBuyDate = false;
                for (KlineData k : klineList) {
                    if (!foundBuyDate) {
                        foundBuyDate = k.getDate().equals(buyDate);
                        if (!foundBuyDate) continue;
                    }
                    if (k.getHigh() > initialHighest) {
                        initialHighest = k.getHigh();
                    }
                }

                double currentPrice = s.getCurrentPrice();
                if (currentPrice > initialHighest) {
                    initialHighest = currentPrice;
                }

                engine = new TrailingStopEngine(s.getBuyPrice(), s.getTrailingPercent(),
                        s.getStopLossPercent(), s.getTargetProfitPercent(), s.isUseGraded());
                if (initialHighest > s.getBuyPrice()) {
                    engine.updateHighestPrice(initialHighest);
                }

                TrailingStopEngine.ActionResult result = engine.updatePrice(currentPrice);
                updateFromResult(result, currentPrice);
            }
        });
    }

    public void saveTradeParams(String name, String code, double buyPrice, String buyDate,
                                double stopLoss, double targetProfit, double trailing, boolean useGraded) {
        settingsManager.saveTradeParams(name, code, buyPrice, buyDate, stopLoss, targetProfit, trailing, useGraded);
        TradeUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy()
                .stockName(name).stockCode(code).buyPrice(buyPrice).buyDate(buyDate)
                .stopLossPercent(stopLoss).targetProfitPercent(targetProfit)
                .trailingPercent(trailing).useGraded(useGraded)
                .build());
    }

    public void startStrategy(double currentPrice) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        if (state.getStockCode().isEmpty()) {
            toastMessage.setValue("请先输入股票代码");
            return;
        }
        if (state.getBuyPrice() <= 0) {
            toastMessage.setValue("请先输入买入价");
            return;
        }
        if (currentPrice <= 0) {
            toastMessage.setValue("请输入有效当前股价");
            return;
        }

        final String code = state.getStockCode();
        final String stockName = state.getStockName();
        final double buyP = state.getBuyPrice();
        final String buyDateStr = state.getBuyDate();
        final double trailing = state.getTrailingPercent() > 0 ? state.getTrailingPercent() : 5.0;
        final double stopLoss = state.getStopLossPercent();
        final double targetProfit = state.getTargetProfitPercent();
        final boolean useGraded = state.isUseGraded();

        if (buyDateStr != null && !buyDateStr.isEmpty()) {
            toastMessage.setValue("正在获取K线数据以计算买入以来最高价...");
            EastMoneyApi.fetchKline(code, 365, new EastMoneyApi.Callback<List<KlineData>>() {
                @Override
                public void onResult(List<KlineData> klineList) {
                    double initialHighest = buyP;
                    boolean foundBuyDate = buyDateStr.isEmpty();
                    for (KlineData k : klineList) {
                        if (!foundBuyDate) {
                            foundBuyDate = k.getDate().equals(buyDateStr);
                            if (!foundBuyDate) continue;
                        }
                        if (k.getHigh() > initialHighest) {
                            initialHighest = k.getHigh();
                        }
                    }
                    if (currentPrice > initialHighest) {
                        initialHighest = currentPrice;
                    }
                    initEngineAndStart(buyP, trailing, stopLoss, targetProfit, useGraded,
                            initialHighest, currentPrice, code, stockName);
                }
            });
        } else {
            initEngineAndStart(buyP, trailing, stopLoss, targetProfit, useGraded,
                    buyP, currentPrice, code, stockName);
        }
    }

    private void initEngineAndStart(double buyP, double trailing, double stopLoss,
                                     double targetProfit, boolean useGraded,
                                     double initialHighest, double currentPrice,
                                     String code, String name) {
        engine = new TrailingStopEngine(buyP, trailing, stopLoss, targetProfit, useGraded);
        if (initialHighest > buyP) {
            engine.updateHighestPrice(initialHighest);
        }

        settingsManager.setIsActive(true);
        settingsManager.setCurrentPrice(currentPrice);

        TrailingStopEngine.ActionResult result = engine.updatePrice(currentPrice);
        updateFromResult(result, currentPrice);

        TradeRepository.appendLog(getApplication().getCacheDir(),
                String.format("启动交易: %s(%s), 买入价=%.2f, 买入以来最高价=%.2f, 当前价=%.2f",
                        name, code, buyP, initialHighest, currentPrice));

        toastMessage.setValue("策略已启动");
    }

    public void updateCurrentPrice(double currentPrice) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        settingsManager.setCurrentPrice(currentPrice);

        if (engine != null && engine.isActive()) {
            TrailingStopEngine.ActionResult result = engine.updatePrice(currentPrice);
            updateFromResult(result, currentPrice);
            savePriceRecord(currentPrice, result);
        } else {
            TradeUiState.Builder builder = state.copy().currentPrice(currentPrice);
            recalcDashboardFields(builder, currentPrice);
            uiState.setValue(builder.build());
        }
    }

    public void fetchRealtimePrice() {
        TradeUiState state = uiState.getValue();
        if (state == null || state.getStockCode().isEmpty()) {
            toastMessage.setValue("请先输入股票代码");
            return;
        }

        EastMoneyApi.fetchRealtime(state.getStockCode(), new EastMoneyApi.Callback<RealtimeQuote>() {
            @Override
            public void onResult(RealtimeQuote quote) {
                if (quote.getPrice() > 0) {
                    TradeUiState current = uiState.getValue();
                    if (current != null) {
                        uiState.setValue(current.copy().currentPrice(quote.getPrice()).build());
                    }
                    toastMessage.setValue("实时价: " + String.format("%.2f", quote.getPrice()));
                    if (engine != null && engine.isActive()) {
                        updateCurrentPrice(quote.getPrice());
                    }
                } else if (quote.getName() != null && !quote.getName().isEmpty()) {
                    toastMessage.setValue(quote.getName() + " 休市或停牌，暂无实时价");
                } else {
                    toastMessage.setValue("获取实时价失败，请检查股票代码: " + state.getStockCode());
                }
            }
        });
    }

    public void resetTrade() {
        engine = null;
        settingsManager.clearDashboard();
        TradeUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy()
                .isActive(false).dashboardVisible(false)
                .highestPrice(0).defenseLine(0).hardStopLine(0)
                .profitPct(0).drawdown(0).distanceToDefense(0)
                .effectiveTrailing(0).isGradedEffect(false)
                .lastMessage("").currentPrice(0)
                .build());
        toastMessage.setValue("交易已重置");
    }

    public void clearCurrentPrice() {
        TradeUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy().currentPrice(0).build());
    }

    private void updateFromResult(TrailingStopEngine.ActionResult result, double currentPrice) {
        TrailingStopEngine.TrailingStopState engineState = result.state;
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        TradeUiState.Builder builder = state.copy()
                .isActive(true)
                .dashboardVisible(true)
                .currentPrice(currentPrice)
                .highestPrice(engineState.highestPrice)
                .defenseLine(engineState.defenseLine)
                .hardStopLine(engineState.hardStopLine)
                .effectiveTrailing(engineState.effectiveTrailing)
                .isGradedEffect(engineState.isGradedEffect)
                .lastMessage(result.message);

        recalcDashboardFields(builder, currentPrice);
        uiState.setValue(builder.build());

        settingsManager.setDefenseLine(engineState.defenseLine);
        settingsManager.setHardStopLine(engineState.hardStopLine);
        settingsManager.setDashboardVisible(true);

        switch (result.actionType) {
            case HARD_STOP:
                settingsManager.setIsActive(false);
                uiState.setValue(uiState.getValue().copy().isActive(false).build());
                alertDialog.setValue("⛔ " + result.message);
                saveExitRecord(currentPrice, "绝对止损");
                break;
            case TRAILING_STOP:
                settingsManager.setIsActive(false);
                uiState.setValue(uiState.getValue().copy().isActive(false).build());
                alertDialog.setValue("🛑 " + result.message);
                saveExitRecord(currentPrice, "动态止盈");
                break;
            case TARGET_REACHED:
                alertDialog.setValue("🎯 " + result.message);
                break;
            case MILESTONE_REACHED:
                toastMessage.setValue("🏆 " + result.message);
                break;
            case DRAWDOWN_CRITICAL:
                toastMessage.setValue("⚠️ " + result.message);
                break;
            case NORMAL:
                break;
        }

        TradeRepository.appendLog(getApplication().getCacheDir(), result.message);
    }

    private void recalcDashboardFields(TradeUiState.Builder builder, double currentPrice) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        double buyP = state.getBuyPrice();
        if (buyP > 0 && currentPrice > 0) {
            double profit = (currentPrice - buyP) / buyP * 100.0;
            builder.profitPct(profit);
        }

        double highest = state.getHighestPrice();
        if (highest > 0 && currentPrice > 0) {
            double dd = (highest - currentPrice) / highest * 100.0;
            builder.drawdown(dd);
        }

        double defLine = state.getDefenseLine();
        if (defLine > 0 && currentPrice > 0) {
            double dist = (currentPrice - defLine) / defLine * 100.0;
            builder.distanceToDefense(dist);
        }
    }

    private void savePriceRecord(double currentPrice, TrailingStopEngine.ActionResult result) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;

        PriceRecord record = new PriceRecord(
                dateFmt.format(new Date()),
                state.getStockCode(), state.getStockName(),
                currentPrice, state.getBuyPrice(),
                result.state.highestPrice, result.state.defenseLine,
                result.state.hardStopLine,
                state.getBuyPrice() > 0 ? (currentPrice - state.getBuyPrice()) / state.getBuyPrice() * 100.0 : 0,
                result.state.highestPrice > 0 ?
                        (result.state.highestPrice - currentPrice) / result.state.highestPrice * 100.0 : 0,
                result.actionType.name()
        );
        PriceHistoryRepository.appendRecord(getApplication().getCacheDir(), record);
    }

    private void saveExitRecord(double exitPrice, String reason) {
        TradeUiState state = uiState.getValue();
        if (state == null || engine == null) return;

        double profitPct = state.getBuyPrice() > 0 ?
                (exitPrice - state.getBuyPrice()) / state.getBuyPrice() * 100.0 : 0;

        TradeRepository.saveRecord(
                getApplication().getCacheDir(),
                state.getStockName(), state.getStockCode(),
                state.getBuyPrice(), engine.getCurrentState().highestPrice,
                exitPrice, profitPct, reason
        );
    }

    public void updateStockInfo(String name, String code) {
        TradeUiState state = uiState.getValue();
        if (state == null) return;
        uiState.setValue(state.copy()
                .stockName(name).stockCode(code)
                .highestPrice(0).defenseLine(0).hardStopLine(0)
                .profitPct(0).drawdown(0).distanceToDefense(0)
                .dashboardVisible(false).isActive(false)
                .currentPrice(0)
                .build());
        engine = null;
        settingsManager.clearDashboard();
        settingsManager.setLastStockName(name);
        settingsManager.setLastStockCode(code);
    }
}
