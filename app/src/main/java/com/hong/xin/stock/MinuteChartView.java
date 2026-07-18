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
    private int displayDays = 5;
    private boolean useMinute = true;

    private final Paint priceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float priceTop, priceBottom;
    private float contentLeft, contentRight;
    private double minPrice, maxPrice;

    private float touchX = -1, touchY = -1;
    private int touchIdx = -1;

    private final DecimalFormat df = new DecimalFormat("#0.000");

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
        priceLinePaint.setStrokeWidth(2f);
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

    private String dayOf(String dateStr) {
        if (dateStr.length() >= 10) return dateStr.substring(0, 10).replace("-", "");
        return dateStr;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        contentLeft = getPaddingLeft() + 80;
        contentRight = w - getPaddingRight() - 12;
        priceTop = getPaddingTop() + 8;
        priceBottom = h - getPaddingBottom() - 28;
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
            drawDaySeps(canvas);
            drawPriceLabels(canvas);
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
            for (int i = firstIdx; i < minuteData.size(); i++) {
                double p = minuteData.get(i).getPrice();
                if (p > 0) { if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p; }
            }
        } else {
            for (int i = firstIdx; i < klineData.size(); i++) {
                KlineData k = klineData.get(i);
                if (k.getHigh() > maxPrice) maxPrice = k.getHigh();
                if (k.getLow() > 0 && k.getLow() < minPrice) minPrice = k.getLow();
            }
        }
        if (minPrice == Double.MAX_VALUE) { minPrice = 0; maxPrice = 1; }
        double range = maxPrice - minPrice;
        if (range == 0) range = maxPrice * 0.05f;
        double padding = range * 0.08f;
        minPrice -= padding;
        maxPrice += padding;
        if (minPrice < 0) minPrice = 0;
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
        for (int i = 0; i <= rows; i++) {
            float y = priceTop + (priceBottom - priceTop) * i / (float) rows;
            canvas.drawLine(contentLeft, y, contentRight, y, gridPaint);
            double price = maxPrice - range * i / (double) rows;
            String label = df.format(price);
            float tw = textPaint.measureText(label);
            canvas.drawText(label, contentLeft - tw - 6, y + 8, textPaint);
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
        Path linePath = new Path();
        boolean started = false;
        for (int i = firstIdx; i < minuteData.size(); i++) {
            float x = idxToX(i - firstIdx);
            double price = minuteData.get(i).getPrice();
            if (price <= 0) continue;
            float y = priceToY(price);
            if (!started) { linePath.moveTo(x, y); started = true; }
            else { linePath.lineTo(x, y); }
        }
        if (started) canvas.drawPath(linePath, priceLinePaint);

        Path avgPath = new Path();
        started = false;
        for (int i = firstIdx; i < minuteData.size(); i++) {
            double avg = minuteData.get(i).getAvgPrice();
            if (avg <= 0) continue;
            float x = idxToX(i - firstIdx);
            float y = priceToY(avg);
            if (!started) { avgPath.moveTo(x, y); started = true; }
            else { avgPath.lineTo(x, y); }
        }
        if (started) canvas.drawPath(avgPath, avgLinePaint);
    }

    private void drawKlinePriceLine(Canvas canvas) {
        int firstIdx = getFirstIndex();
        Path linePath = new Path();
        boolean started = false;
        for (int i = 0; i < klineData.size() - firstIdx; i++) {
            float close = (float) klineData.get(firstIdx + i).getClose();
            float x = idxToX(i);
            if (close <= 0) continue;
            float y = priceToY(close);
            if (!started) { linePath.moveTo(x, y); started = true; }
            else { linePath.lineTo(x, y); }
        }
        if (started) canvas.drawPath(linePath, priceLinePaint);
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
        int firstIdx = getFirstIndex();

        if (useMinute && !minuteData.isEmpty()) {
            if (firstIdx < minuteData.size()) {
                double preClose = minuteData.get(firstIdx).getPreClose();
                if (preClose <= 0) preClose = minuteData.get(firstIdx).getPrice();
                if (preClose > 0) {
                    float y = priceToY(preClose);
                    canvas.drawLine(contentLeft, y, contentRight, y, gridPaint);
                }
            }
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
                canvas.drawText(dateLabel, x - dw / 2, priceBottom + 20, dp);
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
                canvas.drawText(dateLabel, x - dw / 2, priceBottom + 20, dp);
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
            canvas.drawText(marker, x - dw / 2, priceBottom + 20, tp);
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
                label = t + " 价:" + df.format(price) + " 均:" + df.format(d.getAvgPrice());
            }
        } else if (!klineData.isEmpty()) {
            int idx = firstIdx + touchIdx;
            if (idx < klineData.size()) {
                KlineData k = klineData.get(idx);
                price = k.getClose();
                String t = k.getDate();
                if (t.length() >= 16) t = t.substring(5, 16).replace("-", "/").replace(" ", "  ");
                label = t + " C:" + df.format(price) + " H:" + df.format(k.getHigh()) + " L:" + df.format(k.getLow());
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
