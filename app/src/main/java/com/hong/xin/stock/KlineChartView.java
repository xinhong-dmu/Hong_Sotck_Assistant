package com.hong.xin.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.hong.xin.stock.data.model.KlineData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class KlineChartView extends View {

    private static final int MAX_DISPLAY = 20;

    private List<KlineData> allData = new ArrayList<>();
    private int displayCount = 20;

    private final Paint candleUpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint candleDownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint candleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossInfoBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint[] maPaints = new Paint[3];
    private final int[] maColors = {Color.parseColor("#FFC107"), Color.parseColor("#2196F3"), Color.parseColor("#9C27B0")};
    private final int[] maPeriods = {5, 10, 20};
    private final String[] maLabels = {"MA5", "MA10", "MA20"};

    private int contentLeft, contentTop, contentRight, contentBottom;
    private int candleWidth;
    private float scaleX = 1f;
    private float minPrice, maxPrice;

    private ScaleGestureDetector scaleDetector;
    private float touchX = -1, touchY = -1;
    private int touchIndex = -1;

    public KlineChartView(Context context) {
        super(context);
        init();
    }

    public KlineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        candleUpPaint.setStyle(Paint.Style.FILL);
        candleUpPaint.setColor(Color.parseColor("#FF5252"));

        candleDownPaint.setStyle(Paint.Style.FILL);
        candleDownPaint.setColor(Color.parseColor("#4CAF50"));

        candleLinePaint.setStyle(Paint.Style.STROKE);
        candleLinePaint.setStrokeWidth(1f);
        candleLinePaint.setColor(Color.parseColor("#666666"));

        textPaint.setTextSize(28f);
        textPaint.setColor(Color.parseColor("#666666"));

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8f, 4f}, 0));

        maLinePaint.setStyle(Paint.Style.STROKE);
        maLinePaint.setStrokeWidth(2f);

        for (int i = 0; i < 3; i++) {
            maPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            maPaints[i].setStyle(Paint.Style.STROKE);
            maPaints[i].setStrokeWidth(2f);
            maPaints[i].setColor(maColors[i]);
        }

        crossLinePaint.setStyle(Paint.Style.STROKE);
        crossLinePaint.setStrokeWidth(1f);
        crossLinePaint.setColor(Color.parseColor("#999999"));

        crossInfoBgPaint.setStyle(Paint.Style.FILL);
        crossInfoBgPaint.setColor(Color.argb(200, 50, 50, 50));

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleX *= detector.getScaleFactor();
                scaleX = Math.max(1f, Math.min(4f, scaleX));
                invalidate();
                return true;
            }
        });
    }

    public void setData(List<KlineData> data) {
        this.allData = data != null ? data : new ArrayList<>();
        scaleX = 1f;
        invalidate();
    }

    public void setDisplayCount(int count) {
        this.displayCount = Math.min(count, MAX_DISPLAY);
        scaleX = 1f;
        invalidate();
    }

    private List<KlineData> getDisplayData() {
        if (allData.isEmpty()) return allData;
        int count = (int) (displayCount / scaleX);
        count = Math.max(3, Math.min(allData.size(), count));
        return allData.subList(allData.size() - count, allData.size());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        contentLeft = getPaddingLeft() + 60;
        contentTop = getPaddingTop() + 30;
        contentRight = w - getPaddingRight() - 20;
        contentBottom = h - getPaddingBottom() - 50;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        List<KlineData> data = getDisplayData();
        if (data.isEmpty()) {
            canvas.drawText("暂无数据", contentLeft + 50, contentTop + 100, textPaint);
            return;
        }

        calcPriceRange(data);
        candleWidth = (contentRight - contentLeft) / Math.max(data.size(), 1);
        if (candleWidth < 2) candleWidth = 2;

        drawGrid(canvas, data);
        drawCandles(canvas, data);
        drawMaLines(canvas, data);
        drawPriceLabels(canvas);
        drawDateLabels(canvas, data);
        drawLegends(canvas);

        if (touchX >= 0 && touchIndex >= 0 && touchIndex < data.size()) {
            drawCrossHair(canvas, data.get(touchIndex));
        }
    }

    private void calcPriceRange(List<KlineData> data) {
        minPrice = Float.MAX_VALUE;
        maxPrice = Float.MIN_VALUE;
        for (KlineData k : data) {
            float low = (float) k.getLow();
            float high = (float) k.getHigh();
            if (low > 0 && low < minPrice) minPrice = low;
            if (high > maxPrice) maxPrice = high;
        }
        if (minPrice == Float.MAX_VALUE) { minPrice = 0; maxPrice = 1; }
        float range = maxPrice - minPrice;
        minPrice -= range * 0.05f;
        maxPrice += range * 0.05f;
        if (minPrice < 0) minPrice = 0;
    }

    private float priceToY(float price) {
        float ratio = (price - minPrice) / (maxPrice - minPrice);
        return contentBottom - ratio * (contentBottom - contentTop);
    }

    private void drawGrid(Canvas canvas, List<KlineData> data) {
        int rows = 5;
        for (int i = 0; i <= rows; i++) {
            float y = contentTop + (contentBottom - contentTop) * i / (float) rows;
            canvas.drawLine(contentLeft, y, contentRight, y, gridPaint);

            double price = maxPrice - (maxPrice - minPrice) * i / (double) rows;
            String label = new DecimalFormat("#0.00").format(price);
            canvas.drawText(label, 4, y + 8, textPaint);
        }

        for (int i = 0; i <= data.size(); i++) {
            float x = contentLeft + i * candleWidth;
            canvas.drawLine(x, contentTop, x, contentBottom, gridPaint);
        }
    }

    private void drawCandles(Canvas canvas, List<KlineData> data) {
        int barWidth = (int) (candleWidth * 0.7f);
        if (barWidth < 2) barWidth = 2;

        for (int i = 0; i < data.size(); i++) {
            KlineData k = data.get(i);
            float open = (float) k.getOpen();
            float close = (float) k.getClose();
            float high = (float) k.getHigh();
            float low = (float) k.getLow();

            float centerX = contentLeft + i * candleWidth + candleWidth / 2f;
            float yHigh = priceToY(high);
            float yLow = priceToY(low);
            float yOpen = priceToY(open);
            float yClose = priceToY(close);

            candleLinePaint.setColor(close >= open ? Color.parseColor("#FF5252") : Color.parseColor("#4CAF50"));
            canvas.drawLine(centerX, yHigh, centerX, yLow, candleLinePaint);

            float barTop = Math.min(yOpen, yClose);
            float barBottom = Math.max(yOpen, yClose);
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            if (close >= open) {
                if (close > open) {
                    canvas.drawRect(left, barTop, right, barBottom, candleUpPaint);
                } else {
                    candleLinePaint.setColor(Color.parseColor("#FF5252"));
                    canvas.drawRect(left, barTop - 1, right, barBottom + 1, candleLinePaint);
                }
            } else {
                canvas.drawRect(left, barTop, right, barBottom, candleDownPaint);
            }
        }
    }

    private void drawMaLines(Canvas canvas, List<KlineData> data) {
        for (int p = 0; p < 3; p++) {
            int period = maPeriods[p];
            if (data.size() < period) continue;

            float sum = 0;
            List<Float> maValues = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                sum += (float) data.get(i).getClose();
                if (i >= period) sum -= (float) data.get(i - period).getClose();
                if (i >= period - 1) {
                    maValues.add(sum / period);
                } else {
                    maValues.add(Float.NaN);
                }
            }

            Path path = new Path();
            boolean first = true;
            for (int i = 0; i < maValues.size(); i++) {
                float ma = maValues.get(i);
                if (Float.isNaN(ma)) continue;
                float x = contentLeft + i * candleWidth + candleWidth / 2f;
                float y = priceToY(ma);
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            canvas.drawPath(path, maPaints[p]);
        }
    }

    private void drawPriceLabels(Canvas canvas) {
    }

    private void drawDateLabels(Canvas canvas, List<KlineData> data) {
        int step = Math.max(1, data.size() / 5);
        for (int i = 0; i < data.size(); i += step) {
            String date = data.get(i).getDate();
            if (date.length() > 5) date = date.substring(date.length() - 5);
            float x = contentLeft + i * candleWidth + candleWidth / 2f;
            float tw = textPaint.measureText(date);
            canvas.drawText(date, x - tw / 2, contentBottom + 35, textPaint);
        }
    }

    private void drawLegends(Canvas canvas) {
        float x = contentLeft + 10;
        float y = contentTop - 6;

        for (int i = 0; i < 3; i++) {
            if (getDisplayData().size() < maPeriods[i]) continue;
            String label = maLabels[i];
            float tw = textPaint.measureText(label);
            Paint p = new Paint(maPaints[i]);
            p.setStrokeWidth(3f);
            canvas.drawLine(x, y, x + 22, y, p);
            x += 26;
            Paint tp = new Paint(textPaint);
            tp.setTextSize(24f);
            tp.setColor(maColors[i]);
            canvas.drawText(label, x, y + 8, tp);
            x += tw + 18;
        }
    }

    private void drawCrossHair(Canvas canvas, KlineData k) {
        float centerX = contentLeft + touchIndex * candleWidth + candleWidth / 2f;
        canvas.drawLine(centerX, contentTop, centerX, contentBottom, crossLinePaint);
        canvas.drawLine(contentLeft, touchY, contentRight, touchY, crossLinePaint);

        String info = k.getDate() + " O:" + new DecimalFormat("#0.00").format(k.getOpen())
                + " H:" + new DecimalFormat("#0.00").format(k.getHigh())
                + " L:" + new DecimalFormat("#0.00").format(k.getLow())
                + " C:" + new DecimalFormat("#0.00").format(k.getClose());
        float tw = textPaint.measureText(info);

        float bgX = Math.max(0, centerX - tw / 2 - 8);
        float bgW = tw + 16;
        if (bgX + bgW > getWidth()) bgX = getWidth() - bgW;
        if (bgX < 0) bgX = 0;

        Rect bgRect = new Rect((int) bgX, (int) contentBottom + 2,
                (int) (bgX + bgW), (int) contentBottom + 44);
        canvas.drawRect(bgRect, crossInfoBgPaint);

        Paint infoPaint = new Paint(textPaint);
        infoPaint.setColor(Color.WHITE);
        canvas.drawText(info, bgX + 8, contentBottom + 34, infoPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                touchY = event.getY();
                if (touchX >= contentLeft && touchX <= contentRight) {
                    touchIndex = (int) ((touchX - contentLeft) / candleWidth);
                    if (touchIndex >= getDisplayData().size()) touchIndex = getDisplayData().size() - 1;
                    if (touchIndex < 0) touchIndex = 0;
                } else {
                    touchIndex = -1;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchX = -1;
                touchY = -1;
                touchIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
