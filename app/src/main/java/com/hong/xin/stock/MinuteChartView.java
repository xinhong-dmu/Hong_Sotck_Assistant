package com.hong.xin.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MinuteChartView extends View {

    private List<KlineData> klineData = new ArrayList<>();
    private List<MinuteLineData> minuteData = new ArrayList<>();
    private List<Double> purchasePrices = new ArrayList<>();
    private int displayDays = 5;
    private boolean useMinute = true;

    private final Paint priceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ma5Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ma20Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float priceTop, priceBottom;
    private float contentLeft, contentRight;
    private double minPrice, maxPrice;

    private float touchX = -1, touchY = -1;
    private int touchIdx = -1;

    private final DecimalFormat df = new DecimalFormat("#0.000");
    private final DecimalFormat pctDf = new DecimalFormat("+#0.00;-#0.00");

    public MinuteChartView(Context context) {
        super(context);
        init();
    }

    public MinuteChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        priceLinePaint.setStyle(Paint.Style.STROKE);
        priceLinePaint.setStrokeWidth(3f);
        priceLinePaint.setColor(Color.parseColor("#2196F3"));
        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setStrokeWidth(1.5f);
        avgLinePaint.setColor(Color.parseColor("#FFC107"));
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.parseColor("#888888"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(Color.parseColor("#E8E8E8"));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{6f, 3f}, 0));
        crossLinePaint.setStyle(Paint.Style.STROKE);
        crossLinePaint.setStrokeWidth(1f);
        crossLinePaint.setColor(Color.parseColor("#999999"));
        crossBgPaint.setStyle(Paint.Style.FILL);
        crossBgPaint.setColor(Color.argb(200, 50, 50, 50));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(30, 33, 150, 243));
        ma5Paint.setStyle(Paint.Style.STROKE);
        ma5Paint.setStrokeWidth(1.5f);
        ma5Paint.setColor(Color.parseColor("#FFC107"));
        ma20Paint.setStyle(Paint.Style.STROKE);
        ma20Paint.setStrokeWidth(1.5f);
        ma20Paint.setColor(Color.parseColor("#9C27B0"));
    }

    private int dataSize() {
        return getDisplayCount();
    }

    private int getDisplayCount() {
        if (useMinute) {
            int count = 0;
            String endDay = "";
            for (int i = minuteData.size() - 1; i >= 0; i--) {
                String d = dayOf(minuteData.get(i).getTime());
                if (!d.equals(endDay)) { endDay = d; count++; if (count > displayDays) break; }
            }
            int firstIdx = getFirstIndex();
            return minuteData.size() - firstIdx;
        } else {
            int from = Math.max(0, klineData.size() - displayDays * 48);
            return klineData.size() - from;
        }
    }

    private int getFirstIndex() {
        if (useMinute) {
            String endDay = "";
            int count = 0;
            for (int i = minuteData.size() - 1; i >= 0; i--) {
                String d = dayOf(minuteData.get(i).getTime());
                if (endDay.isEmpty()) { endDay = d; count = 1; }
                else if (!d.equals(endDay)) { endDay = d; count++; }
                if (count > displayDays) return i + 1;
            }
            return 0;
        }
        return Math.max(0, klineData.size() - displayDays * 48);
    }

    private float chartScaleX() {
        int n = getDisplayCount();
        if (n <= 0) return 1f;
        float viewW = contentRight - contentLeft;
        if (viewW <= 0) return 1f;
        return viewW / n;
    }

    public void setData(List<MinuteLineData> data) {
        this.minuteData = data != null ? data : new ArrayList<>();
        this.useMinute = !this.minuteData.isEmpty();
        invalidate();
    }

    public void setKlineData(List<KlineData> data) {
        this.klineData = data != null ? data : new ArrayList<>();
        this.useMinute = false;
        invalidate();
    }

    public void setDisplayDays(int days) {
        this.displayDays = days;
        invalidate();
    }

    public void setPurchasePrices(List<Double> prices) {
        this.purchasePrices = prices != null ? prices : new ArrayList<>();
        invalidate();
    }

    private String dayOf(String dateStr) {
        if (dateStr.length() >= 10) return dateStr.substring(0, 10).replace("-", "");
        return dateStr;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        contentLeft = getPaddingLeft() + 80;
        contentRight = w - getPaddingRight() - 65;
        priceTop = getPaddingTop() + 8;
        priceBottom = h - getPaddingBottom() - 40;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (dataSize() == 0) {
                canvas.drawText("暂无数据", contentLeft + 50, priceTop + 100, textPaint);
                return;
            }
            calcRange();
            drawPriceGrid(canvas);
            drawPriceLine(canvas);
            if (!useMinute) drawKlineLegends(canvas);
            drawDaySeps(canvas);
            drawPriceLabels(canvas);
            drawPurchaseLines(canvas);
            drawDateLabels(canvas);
            drawCrosshair(canvas);
        } catch (Exception e) {
            Log.e("MinuteChartView", "onDraw error: " + e.getMessage(), e);
        }
    }

    private void calcRange() {
        minPrice = Double.MAX_VALUE;
        maxPrice = Double.MIN_VALUE;
        int firstIdx = getFirstIndex();
        if (useMinute) {
            double preClose = 0;
            for (int i = firstIdx; i < minuteData.size(); i++) {
                preClose = minuteData.get(i).getPreClose();
                if (preClose > 0) break;
            }
            if (preClose > 0) {
                double maxPct = 0;
                for (int i = firstIdx; i < minuteData.size(); i++) {
                    double p = minuteData.get(i).getPrice();
                    if (p > 0) {
                        double pct = Math.abs((p - preClose) / preClose * 100);
                        if (pct > maxPct) maxPct = pct;
                    }
                }
                double padding = Math.max(maxPct * 0.08, 0.3);
                maxPct += padding;
                minPrice = preClose * (1 - maxPct / 100);
                maxPrice = preClose * (1 + maxPct / 100);
            } else {
                for (int i = firstIdx; i < minuteData.size(); i++) {
                    double p = minuteData.get(i).getPrice();
                    if (p > 0) { if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p; }
                }
            }
        } else {
            int size = klineData.size();
            int maPeriod = displayDays <= 5 ? 5 : 20;
            double preClose = 0;
            if (firstIdx < klineData.size()) {
                KlineData first = klineData.get(firstIdx);
                if (first.getChange() != 0) preClose = first.getClose() - first.getChange();
                else preClose = first.getClose() / (1 + first.getPctChg() / 100);
            }
            if (preClose > 0) {
                double maxPct = 0;
                for (int i = firstIdx; i < size; i++) {
                    KlineData k = klineData.get(i);
                    if (k.getHigh() > 0) {
                        double pct = Math.abs((k.getHigh() - preClose) / preClose * 100);
                        if (pct > maxPct) maxPct = pct;
                    }
                    if (k.getLow() > 0) {
                        double pct = Math.abs((k.getLow() - preClose) / preClose * 100);
                        if (pct > maxPct) maxPct = pct;
                    }
                }
                double sumMa = 0;
                for (int i = 0; i < size; i++) {
                    sumMa += klineData.get(i).getClose();
                    if (i >= maPeriod) sumMa -= klineData.get(i - maPeriod).getClose();
                    if (i >= firstIdx && i >= maPeriod - 1) {
                        double ma = sumMa / maPeriod;
                        double pct = Math.abs((ma - preClose) / preClose * 100);
                        if (pct > maxPct) maxPct = pct;
                    }
                }
                double padding = Math.max(maxPct * 0.08, 0.3);
                maxPct += padding;
                minPrice = preClose * (1 - maxPct / 100);
                maxPrice = preClose * (1 + maxPct / 100);
            } else {
                for (int i = firstIdx; i < size; i++) {
                    KlineData k = klineData.get(i);
                    if (k.getHigh() > maxPrice) maxPrice = k.getHigh();
                    if (k.getLow() > 0 && k.getLow() < minPrice) minPrice = k.getLow();
                }
            }
        }
        if (minPrice == Double.MAX_VALUE) { minPrice = 0; maxPrice = 1; }
        for (double p : purchasePrices) {
            if (p > 0) {
                if (p < minPrice) minPrice = p;
                if (p > maxPrice) maxPrice = p;
            }
        }
        double range = maxPrice - minPrice;
        if (range == 0) range = maxPrice * 0.05f;
        double padding = range * 0.08f;
        minPrice -= padding;
        maxPrice += padding;
        if (minPrice < 0) minPrice = 0;
    }

    private double getPreClosePrice() {
        if (useMinute && !minuteData.isEmpty()) {
            int firstIdx = getFirstIndex();
            if (firstIdx < minuteData.size()) {
                double pc = minuteData.get(firstIdx).getPreClose();
                if (pc > 0) return pc;
                pc = minuteData.get(firstIdx).getPrice();
                if (pc > 0) return pc;
            }
        } else if (!klineData.isEmpty()) {
            int firstIdx = getFirstIndex();
            if (firstIdx < klineData.size()) {
                KlineData first = klineData.get(firstIdx);
                if (first.getChange() != 0) return first.getClose() - first.getChange();
                double pc = first.getClose() / (1 + first.getPctChg() / 100);
                if (pc > 0) return pc;
            }
        }
        return 0;
    }

    private float priceToY(double price) {
        double range = maxPrice - minPrice;
        if (range <= 0) return priceBottom;
        return (float) (priceBottom - (price - minPrice) / range * (priceBottom - priceTop));
    }

    private float idxToX(int idx) {
        float scaleX = chartScaleX();
        return contentLeft + idx * scaleX;
    }

    private void drawPriceGrid(Canvas canvas) {
        int rows = 5;
        double range = maxPrice - minPrice;
        double preClose = getPreClosePrice();
        Paint pctPaint = new Paint(textPaint);
        pctPaint.setTextSize(22f);
        for (int i = 0; i <= rows; i++) {
            float y = priceTop + (priceBottom - priceTop) * i / (float) rows;
            canvas.drawLine(contentLeft, y, contentRight, y, gridPaint);
            double price = maxPrice - range * i / (double) rows;
            String label = df.format(price);
            float tw = textPaint.measureText(label);
            canvas.drawText(label, contentLeft - tw - 6, y + 8, textPaint);
            if (preClose > 0) {
                double pct = (price - preClose) / preClose * 100;
                String pctLabel = pctDf.format(pct) + "%";
                canvas.drawText(pctLabel, contentRight + 6, y + 8, pctPaint);
            }
        }
    }

    private void drawPriceLine(Canvas canvas) {
        if (useMinute) {
            drawMinutePriceLine(canvas);
        } else {
            drawKlinePriceLine(canvas);
        }
    }

    private void drawMinutePriceLine(Canvas canvas) {
        int firstIdx = getFirstIndex();
        if (firstIdx >= minuteData.size()) return;

        Paint segPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        segPaint.setStyle(Paint.Style.STROKE);
        segPaint.setStrokeWidth(3f);
        segPaint.setStrokeCap(Paint.Cap.ROUND);

        Float prevX = null, prevY = null;
        for (int i = firstIdx; i < minuteData.size(); i++) {
            float x = idxToX(i - firstIdx);
            double price = minuteData.get(i).getPrice();
            double avgPrice = minuteData.get(i).getAvgPrice();
            if (price <= 0) { prevX = null; prevY = null; continue; }
            float y = priceToY(price);
            if (prevX != null) {
                int color = Color.parseColor("#2196F3");
                if (avgPrice > 0) {
                    color = price >= avgPrice ? Color.parseColor("#43A047") : Color.parseColor("#E53935");
                }
                segPaint.setColor(color);
                canvas.drawLine(prevX, prevY, x, y, segPaint);
            }
            prevX = x;
            prevY = y;
        }
    }

    private void drawKlinePriceLine(Canvas canvas) {
        int firstIdx = getFirstIndex();
        int displayCount = klineData.size() - firstIdx;
        Path linePath = new Path();
        boolean started = false;
        for (int i = 0; i < displayCount; i++) {
            float close = (float) klineData.get(firstIdx + i).getClose();
            float x = idxToX(i);
            if (close <= 0) continue;
            float y = priceToY(close);
            if (!started) { linePath.moveTo(x, y); started = true; }
            else { linePath.lineTo(x, y); }
        }
        if (started) canvas.drawPath(linePath, priceLinePaint);

        if (displayDays <= 5) {
            drawMaLine(canvas, firstIdx, displayCount, 5, ma5Paint);
        } else {
            drawMaLine(canvas, firstIdx, displayCount, 20, ma20Paint);
        }
    }

    private void drawMaLine(Canvas canvas, int firstIdx, int displayCount, int period, Paint paint) {
        if (klineData.size() < period) return;
        int start = Math.max(0, firstIdx - period + 1);
        float sum = 0;
        for (int i = start; i < firstIdx; i++) {
            sum += (float) klineData.get(i).getClose();
        }
        Path path = new Path();
        boolean started = false;
        for (int i = 0; i < displayCount; i++) {
            int idx = firstIdx + i;
            float close = (float) klineData.get(idx).getClose();
            sum += close;
            if (idx >= period) sum -= (float) klineData.get(idx - period).getClose();
            if (idx < period - 1) continue;
            float ma = sum / period;
            float x = idxToX(i);
            float y = priceToY(ma);
            if (!started) { path.moveTo(x, y); started = true; }
            else { path.lineTo(x, y); }
        }
        if (started) canvas.drawPath(path, paint);
    }

    private double calcMA(int idx, int period) {
        if (idx < period - 1 || klineData.isEmpty()) return 0;
        double sum = 0;
        for (int i = idx - period + 1; i <= idx; i++) {
            sum += klineData.get(i).getClose();
        }
        return sum / period;
    }

    private void drawKlineLegends(Canvas canvas) {
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setTextSize(22f);
        float x = contentLeft + 8;
        float y = priceTop + 16;
        float segLen = 18, textGap = 4;

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);

        int total = klineData.size();

        linePaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawLine(x, y, x + segLen, y, linePaint);
        lp.setColor(Color.parseColor("#2196F3"));
        canvas.drawText("价格", x + segLen + textGap, y + 8, lp);
        x += segLen + textGap + lp.measureText("价格") + 6;

        if (displayDays <= 5 && total >= 5) {
            linePaint.setColor(Color.parseColor("#FFC107"));
            canvas.drawLine(x, y, x + segLen, y, linePaint);
            lp.setColor(Color.parseColor("#FFC107"));
            canvas.drawText("MA5", x + segLen + textGap, y + 8, lp);
        } else if (displayDays > 5 && total >= 20) {
            linePaint.setColor(Color.parseColor("#9C27B0"));
            canvas.drawLine(x, y, x + segLen, y, linePaint);
            lp.setColor(Color.parseColor("#9C27B0"));
            canvas.drawText("MA20", x + segLen + textGap, y + 8, lp);
        }
    }

    private void drawDaySeps(Canvas canvas) {
        Paint sepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sepPaint.setStyle(Paint.Style.STROKE);
        sepPaint.setStrokeWidth(1f);
        sepPaint.setColor(Color.parseColor("#CCCCCC"));
        sepPaint.setPathEffect(new DashPathEffect(new float[]{4f, 4f}, 0));

        int firstIdx = getFirstIndex();
        if (useMinute && !minuteData.isEmpty()) {
            String lastDay = "";
            for (int i = firstIdx; i < minuteData.size(); i++) {
                String day = dayOf(minuteData.get(i).getTime());
                if (day.isEmpty() || day.equals(lastDay)) continue;
                lastDay = day;
                float x = idxToX(i - firstIdx);
                if (x > contentLeft && x < contentRight) {
                    canvas.drawLine(x, priceTop, x, priceBottom, sepPaint);
                }
            }
        } else if (!klineData.isEmpty()) {
            String lastDay = "";
            for (int i = 0; i < klineData.size() - firstIdx; i++) {
                String day = dayOf(klineData.get(firstIdx + i).getDate());
                if (day.isEmpty() || day.equals(lastDay)) continue;
                lastDay = day;
                float x = idxToX(i);
                if (x > contentLeft && x < contentRight) {
                    canvas.drawLine(x, priceTop, x, priceBottom, sepPaint);
                }
            }
        }
    }

    private void drawPriceLabels(Canvas canvas) {
        double preClose = getPreClosePrice();
        if (preClose > 0) {
            float y = priceToY(preClose);
            Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            centerPaint.setStyle(Paint.Style.STROKE);
            centerPaint.setStrokeWidth(1.5f);
            centerPaint.setColor(Color.parseColor("#666666"));
            canvas.drawLine(contentLeft, y, contentRight, y, centerPaint);
        }
    }

    private void drawPurchaseLines(Canvas canvas) {
        if (purchasePrices.isEmpty()) return;
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(Color.parseColor("#FF9800"));
        linePaint.setPathEffect(new DashPathEffect(new float[]{8f, 4f}, 0));

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(22f);
        labelPaint.setColor(Color.parseColor("#FF9800"));

        for (double price : purchasePrices) {
            if (price <= 0) continue;
            float y = priceToY(price);
            if (y < priceTop || y > priceBottom) continue;
            canvas.drawLine(contentLeft, y, contentRight, y, linePaint);
            String label = "买入 " + df.format(price);
            float tw = labelPaint.measureText(label);
            canvas.drawText(label, contentRight - tw - 4, y - 6, labelPaint);
        }
    }

    private void drawDateLabels(Canvas canvas) {
        int firstIdx = getFirstIndex();
        if (useMinute && !minuteData.isEmpty()) {
            String lastDay = ""; float lastLabelX = -1000;

            for (int i = firstIdx; i < minuteData.size(); i++) {
                String day = dayOf(minuteData.get(i).getTime());
                if (day.isEmpty() || day.equals(lastDay)) continue;
                lastDay = day;
                float x = idxToX(i - firstIdx);
                if (x < contentLeft || x > contentRight || x - lastLabelX < 80) continue;
                String dateLabel = day.length() == 8 ? day.substring(4, 6) + "/" + day.substring(6, 8) : day;
                Paint dp = new Paint(textPaint);
                dp.setColor(Color.parseColor("#666666"));
                dp.setTextSize(20f);
                float dw = dp.measureText(dateLabel);
                canvas.drawText(dateLabel, x - dw / 2, priceBottom + 22, dp);
                lastLabelX = x;
            }

            drawTimeAxis(canvas, firstIdx);
        } else if (!klineData.isEmpty()) {
            String lastDay = ""; float lastLabelX = -1000;
            for (int i = 0; i < klineData.size() - firstIdx; i++) {
                String date = klineData.get(firstIdx + i).getDate();
                String day = dayOf(date);
                if (day.isEmpty() || day.equals(lastDay)) continue;
                lastDay = day;
                float x = idxToX(i);
                if (x < contentLeft || x > contentRight || x - lastLabelX < 60) continue;
                String dateLabel = day.length() == 8 ? day.substring(4, 6) + "/" + day.substring(6, 8) : day;
                Paint dp = new Paint(textPaint);
                dp.setColor(Color.parseColor("#666666"));
                dp.setTextSize(20f);
                float dw = dp.measureText(dateLabel);
                canvas.drawText(dateLabel, x - dw / 2, priceBottom + 35, dp);
                lastLabelX = x;
            }
        }
    }

    private void drawTimeAxis(Canvas canvas, int firstIdx) {
        String[] markers = {"09:30", "10:00", "10:30", "11:00", "11:30", "13:00", "13:30", "14:00", "14:30", "15:00"};

        Paint tp = new Paint(textPaint);
        tp.setColor(Color.parseColor("#AAAAAA"));
        tp.setTextSize(20f);

        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1f);
        tickPaint.setColor(Color.parseColor("#E5E5E5"));

        float lastLabelX = -1000;

        for (String marker : markers) {
            int dataIdx = -1;
            for (int i = firstIdx; i < minuteData.size(); i++) {
                String t = minuteData.get(i).getTime();
                if (t.length() < 16) continue;
                String hhmm = t.substring(11, 16);
                if (hhmm.compareTo(marker) >= 0) {
                    dataIdx = i;
                    break;
                }
            }
            if (dataIdx < 0) continue;

            float x = idxToX(dataIdx - firstIdx);
            if (x < contentLeft + 4 || x > contentRight - 4) continue;
            if (x - lastLabelX < 45) continue;

            float dw = tp.measureText(marker);
            canvas.drawText(marker, x - dw / 2, priceBottom + 35, tp);
            canvas.drawLine(x, priceTop, x, priceBottom, tickPaint);
            lastLabelX = x;
        }
    }

    private void drawCrosshair(Canvas canvas) {
        if (dataSize() == 0 || touchIdx < 0) return;
        double price = 0;
        String label = "";

        int firstIdx = getFirstIndex();
        if (useMinute && !minuteData.isEmpty()) {
            int idx = firstIdx + touchIdx;
            if (idx < minuteData.size()) {
                MinuteLineData d = minuteData.get(idx);
                price = d.getPrice();
                String t = d.getTime();
                if (t.length() >= 16) t = t.substring(5, 10) + " " + t.substring(11, 16);
                label = t + " " + df.format(price);
            }
        } else if (!klineData.isEmpty()) {
            int idx = firstIdx + touchIdx;
            if (idx < klineData.size()) {
                KlineData k = klineData.get(idx);
                price = k.getClose();
                String t = k.getDate();
                if (t.length() >= 16) t = t.substring(5, 16).replace("-", "/").replace(" ", "  ");
                label = t + " " + df.format(price);
                if (displayDays <= 5 && idx >= 4) {
                    label += " MA5:" + df.format(calcMA(idx, 5));
                } else if (displayDays > 5 && idx >= 19) {
                    label += " MA20:" + df.format(calcMA(idx, 20));
                }
            }
        }

        if (price <= 0) return;
        float x = idxToX(touchIdx);
        x = Math.max(contentLeft, Math.min(contentRight, x));
        float y = priceToY(price);
        canvas.drawLine(x, priceTop, x, priceBottom, crossLinePaint);
        canvas.drawLine(contentLeft, y, contentRight, y, crossLinePaint);

        Paint ip = new Paint(textPaint);
        ip.setColor(Color.WHITE);
        ip.setTextSize(22f);
        float tw = ip.measureText(label);
        float bgX = Math.max(0, x - tw / 2 - 8);
        float bgW = tw + 16;
        if (bgX + bgW > getWidth()) bgX = getWidth() - bgW;
        if (bgX < 0) bgX = 0;
        canvas.drawRect(new Rect((int) bgX, (int) priceTop, (int) (bgX + bgW), (int) priceTop + 36), crossBgPaint);
        canvas.drawText(label, bgX + 8, priceTop + 26, ip);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchX = x; touchY = y;
                if (x >= contentLeft && x <= contentRight) {
                    touchIdx = (int) ((x - contentLeft) / chartScaleX());
                    int max = getDisplayCount() - 1;
                    if (max < 0) max = 0;
                    if (touchIdx < 0) touchIdx = 0;
                    if (touchIdx > max) touchIdx = max;
                } else { touchIdx = -1; }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchX = -1; touchY = -1; touchIdx = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
