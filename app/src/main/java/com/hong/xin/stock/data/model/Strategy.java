package com.hong.xin.stock.data.model;

public class Strategy {

    private String id;
    private String stockCode;
    private String stockName;
    private String name;
    private String signalType;
    private String direction;
    private String conditionText;
    private String aiMessage;
    private double targetPrice;
    private double stopLossPrice;
    private double conditionPriceAbove;
    private double conditionPriceBelow;
    private double conditionPctChangeUp;
    private double conditionPctChangeDown;
    private int conditionMaAbove;
    private int conditionMaBelow;
    private double conditionVolumeRatioMin;
    private double conditionVolumeRatioMax;
    private long createdAt;
    private boolean isActive;
    private long lastMatchedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getConditionText() { return conditionText; }
    public void setConditionText(String conditionText) { this.conditionText = conditionText; }

    public String getAiMessage() { return aiMessage; }
    public void setAiMessage(String aiMessage) { this.aiMessage = aiMessage; }

    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }

    public double getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(double stopLossPrice) { this.stopLossPrice = stopLossPrice; }

    public double getConditionPriceAbove() { return conditionPriceAbove; }
    public void setConditionPriceAbove(double conditionPriceAbove) { this.conditionPriceAbove = conditionPriceAbove; }

    public double getConditionPriceBelow() { return conditionPriceBelow; }
    public void setConditionPriceBelow(double conditionPriceBelow) { this.conditionPriceBelow = conditionPriceBelow; }

    public double getConditionPctChangeUp() { return conditionPctChangeUp; }
    public void setConditionPctChangeUp(double conditionPctChangeUp) { this.conditionPctChangeUp = conditionPctChangeUp; }

    public double getConditionPctChangeDown() { return conditionPctChangeDown; }
    public void setConditionPctChangeDown(double conditionPctChangeDown) { this.conditionPctChangeDown = conditionPctChangeDown; }

    public int getConditionMaAbove() { return conditionMaAbove; }
    public void setConditionMaAbove(int conditionMaAbove) { this.conditionMaAbove = conditionMaAbove; }

    public int getConditionMaBelow() { return conditionMaBelow; }
    public void setConditionMaBelow(int conditionMaBelow) { this.conditionMaBelow = conditionMaBelow; }

    public double getConditionVolumeRatioMin() { return conditionVolumeRatioMin; }
    public void setConditionVolumeRatioMin(double conditionVolumeRatioMin) { this.conditionVolumeRatioMin = conditionVolumeRatioMin; }

    public double getConditionVolumeRatioMax() { return conditionVolumeRatioMax; }
    public void setConditionVolumeRatioMax(double conditionVolumeRatioMax) { this.conditionVolumeRatioMax = conditionVolumeRatioMax; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getLastMatchedAt() { return lastMatchedAt; }
    public void setLastMatchedAt(long lastMatchedAt) { this.lastMatchedAt = lastMatchedAt; }
}
