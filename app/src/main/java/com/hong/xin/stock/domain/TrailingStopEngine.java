package com.hong.xin.stock.domain;

public class TrailingStopEngine {

    public enum ActionType {
        NORMAL, HARD_STOP, TRAILING_STOP, TARGET_REACHED, MILESTONE_REACHED, DRAWDOWN_CRITICAL
    }

    public static class TrailingStopState {
        public final double highestPrice;
        public final double defenseLine;
        public final double hardStopLine;
        public final double effectiveTrailing;
        public final boolean isGradedEffect;

        TrailingStopState(double highestPrice, double defenseLine, double hardStopLine,
                          double effectiveTrailing, boolean isGradedEffect) {
            this.highestPrice = highestPrice;
            this.defenseLine = defenseLine;
            this.hardStopLine = hardStopLine;
            this.effectiveTrailing = effectiveTrailing;
            this.isGradedEffect = isGradedEffect;
        }

        public double getHighestPrice() { return highestPrice; }
        public double getDefenseLine() { return defenseLine; }
        public double getHardStopLine() { return hardStopLine; }
        public double getEffectiveTrailing() { return effectiveTrailing; }
        public boolean isGradedEffect() { return isGradedEffect; }
    }

    public static class ActionResult {
        public final ActionType actionType;
        public final TrailingStopState state;
        public final String message;
        public final double milestone;

        ActionResult(ActionType actionType, TrailingStopState state, String message) {
            this(actionType, state, message, 0);
        }

        ActionResult(ActionType actionType, TrailingStopState state, String message, double milestone) {
            this.actionType = actionType;
            this.state = state;
            this.message = message;
            this.milestone = milestone;
        }
    }

    private final double buyPrice;
    private final double baseTrailingPercent;
    private final double stopLossPercent;
    private final double targetProfitPercent;
    private final boolean useGraded;

    private double highestPrice;
    private double defenseLine;
    private double hardStopLine;
    private boolean isActive;

    public TrailingStopEngine(double buyPrice, double baseTrailingPercent, double stopLossPercent,
                              double targetProfitPercent, boolean useGraded) {
        this.buyPrice = buyPrice;
        this.baseTrailingPercent = baseTrailingPercent;
        this.stopLossPercent = stopLossPercent;
        this.targetProfitPercent = targetProfitPercent;
        this.useGraded = useGraded;

        this.highestPrice = buyPrice;
        this.hardStopLine = buyPrice * (1 - stopLossPercent / 100.0);
        this.defenseLine = buyPrice;
        this.isActive = true;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public boolean isActive() {
        return isActive;
    }

    public void updateHighestPrice(double price) {
        if (price > highestPrice) {
            highestPrice = price;
            double currentProfitPct = (highestPrice - buyPrice) / buyPrice * 100.0;
            double effTrailing = calcEffectiveTrailing(currentProfitPct);
            defenseLine = Math.max(highestPrice * (1 - effTrailing / 100.0), hardStopLine);
        }
    }

    private double calcEffectiveTrailing(double currentProfitPct) {
        if (!useGraded) {
            return baseTrailingPercent;
        }
        double eff;
        if (currentProfitPct <= 10) {
            eff = baseTrailingPercent;
        } else if (currentProfitPct <= 20) {
            eff = baseTrailingPercent * 0.6;
        } else if (currentProfitPct <= 30) {
            eff = baseTrailingPercent * 0.4;
        } else {
            eff = baseTrailingPercent * 0.25;
        }
        return Math.max(eff, 1.5);
    }

    public ActionResult updatePrice(double currentPrice) {
        if (!isActive) {
            TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, 0, false);
            return new ActionResult(ActionType.NORMAL, state, "交易已结束");
        }

        double currentProfitPct = (currentPrice - buyPrice) / buyPrice * 100.0;

        if (currentPrice <= hardStopLine) {
            String msg = String.format("触发绝对止损线! 股价=%.2f, 止损线=%.2f, 亏损=%.2f%%", currentPrice, hardStopLine, currentProfitPct);
            isActive = false;
            TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, 0, false);
            return new ActionResult(ActionType.HARD_STOP, state, msg);
        }

        if (currentPrice > highestPrice) {
            highestPrice = currentPrice;
            double effTrailing = calcEffectiveTrailing(currentProfitPct);
            defenseLine = Math.max(highestPrice * (1 - effTrailing / 100.0), hardStopLine);
            String logMsg = String.format("创新高! 最高价=%.2f, 防守线上移至=%.2f (有效回撤=%.1f%%)", highestPrice, defenseLine, effTrailing);

            if (currentProfitPct >= 10 && currentProfitPct < 20) {
                return new ActionResult(ActionType.MILESTONE_REACHED,
                        new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded),
                        "里程碑: 盈利突破10%", 10);
            } else if (currentProfitPct >= 20 && currentProfitPct < 30) {
                return new ActionResult(ActionType.MILESTONE_REACHED,
                        new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded),
                        "里程碑: 盈利突破20%", 20);
            } else if (currentProfitPct >= 30 && currentProfitPct < 50) {
                return new ActionResult(ActionType.MILESTONE_REACHED,
                        new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded),
                        "里程碑: 盈利突破30%", 30);
            } else if (currentProfitPct >= 50 && currentProfitPct < 100) {
                return new ActionResult(ActionType.MILESTONE_REACHED,
                        new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded),
                        "里程碑: 盈利突破50%", 50);
            } else {
                double effTrailingNonGraded = calcEffectiveTrailing(currentProfitPct);
                TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailingNonGraded, useGraded);
                return new ActionResult(ActionType.NORMAL, state, logMsg);
            }
        }

        double effTrailing = calcEffectiveTrailing(currentProfitPct);
        if (currentPrice <= defenseLine) {
            String msg = String.format("触发动态防守线! 股价=%.2f, 防守线=%.2f, 盈利=%.2f%%", currentPrice, defenseLine, currentProfitPct);
            isActive = false;
            TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded);
            return new ActionResult(ActionType.TRAILING_STOP, state, msg);
        }

        if (currentProfitPct >= targetProfitPercent) {
            String msg = String.format("达到目标止盈! 盈利=%.2f%%, 目标=%.1f%%", currentProfitPct, targetProfitPercent);
            TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded);
            return new ActionResult(ActionType.TARGET_REACHED, state, msg);
        }

        double drawdown = (highestPrice - currentPrice) / highestPrice * 100.0;
        if (effTrailing > 0 && drawdown >= effTrailing * 0.8) {
            String logMsg = String.format("高危预警: 回撤=%.2f%%, 已达容忍度(%.1f%%)的80%%", drawdown, effTrailing);
            TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded);
            return new ActionResult(ActionType.DRAWDOWN_CRITICAL, state, logMsg);
        }

        String logMsg = String.format("正常监控: 股价=%.2f, 盈利=%.2f%%, 最高=%.2f, 回撤=%.2f%%", currentPrice, currentProfitPct, highestPrice, drawdown);
        TrailingStopState state = new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded);
        return new ActionResult(ActionType.NORMAL, state, logMsg);
    }

    public TrailingStopState getCurrentState() {
        double effTrailing = calcEffectiveTrailing((highestPrice - buyPrice) / buyPrice * 100.0);
        return new TrailingStopState(highestPrice, defenseLine, hardStopLine, effTrailing, useGraded);
    }
}
