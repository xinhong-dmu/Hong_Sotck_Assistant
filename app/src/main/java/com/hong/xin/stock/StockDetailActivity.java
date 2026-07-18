package com.hong.xin.stock;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hong.xin.stock.data.PurchaseRecordManager;
import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;
import com.hong.xin.stock.data.model.PurchaseRecord;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Stock;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StockDetailActivity extends AppCompatActivity {

    private Stock stock;
    private int currentDays = 5;
    private boolean isIntraday = true;
    private RealtimeQuote quote;

    private TextView nameText, codeText, priceText, changeText;
    private TextView tvOpen, tvHigh, tvLow, tvPreClose;
    private TextView tvVolume, tvAmount, tvPe, tvPb, tvTurnover, tvMarketcap;
    private TextView tvIopv, tvPremium, etfExtraLabel, etfPremiumLabel;
    private TextView tabIntraday, tab5, tab20;
    private MinuteChartView chartIntraday, chartView;

    private EditText etPurchasePrice, etPurchaseDate;
    private TextView btnSavePurchase, btnDeletePurchase, btnAddStock;
    private LinearLayout purchaseInfoLayout;
    private TextView tvCostPrice, tvProfitLoss;

    private TextView btnAiAnalysis;

    private PurchaseRecordManager purchaseRecordManager;
    private PurchaseRecord currentRecord;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        Intent intent = getIntent();
        String code = intent.getStringExtra("code");
        String name = intent.getStringExtra("name");
        String type = intent.getStringExtra("type");
        stock = new Stock(code != null ? code : "", name != null ? name : "", type != null ? type : "");

        initViews();
        bindTabs();
        bindPurchaseActions();
        loadData();
    }

    private void initViews() {
        nameText = findViewById(R.id.stock_name);
        codeText = findViewById(R.id.stock_code);
        priceText = findViewById(R.id.current_price);
        changeText = findViewById(R.id.change_text);

        tvOpen = findViewById(R.id.tv_open);
        tvHigh = findViewById(R.id.tv_high);
        tvLow = findViewById(R.id.tv_low);
        tvPreClose = findViewById(R.id.tv_preclose);
        tvVolume = findViewById(R.id.tv_volume);
        tvAmount = findViewById(R.id.tv_amount);
        tvPe = findViewById(R.id.tv_pe);
        tvPb = findViewById(R.id.tv_pb);
        tvTurnover = findViewById(R.id.tv_turnover);
        tvMarketcap = findViewById(R.id.tv_marketcap);
        tvIopv = findViewById(R.id.tv_iopv);
        tvPremium = findViewById(R.id.tv_premium);
        etfExtraLabel = findViewById(R.id.etf_extra_label);
        etfPremiumLabel = findViewById(R.id.etf_premium_label);

        tabIntraday = findViewById(R.id.tab_intraday);
        tab5 = findViewById(R.id.tab_5d);
        tab20 = findViewById(R.id.tab_20d);
        chartIntraday = findViewById(R.id.chart_intraday);
        chartView = findViewById(R.id.chart_view);

        etPurchasePrice = findViewById(R.id.et_purchase_price);
        etPurchaseDate = findViewById(R.id.et_purchase_date);
        btnSavePurchase = findViewById(R.id.btn_save_purchase);
        btnDeletePurchase = findViewById(R.id.btn_delete_purchase);
        btnAddStock = findViewById(R.id.btn_add_stock);
        purchaseInfoLayout = findViewById(R.id.purchase_info_layout);
        tvCostPrice = findViewById(R.id.tv_cost_price);
        tvProfitLoss = findViewById(R.id.tv_profit_loss);

        btnAiAnalysis = findViewById(R.id.btn_ai_analysis);

        chartView.setVisibility(View.GONE);
        updateAddStockButton();

        nameText.setText(stock.getName());
        codeText.setText(stock.getCode());
    }

    private void bindTabs() {
        tabIntraday.setOnClickListener(v -> switchDays(0));
        tab5.setOnClickListener(v -> switchDays(5));
        tab20.setOnClickListener(v -> switchDays(20));
    }

    private void bindPurchaseActions() {
        purchaseRecordManager = PurchaseRecordManager.getInstance(this);
        loadPurchaseRecord();

        etPurchaseDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String date = year + "-" + String.format(Locale.getDefault(), "%02d", month + 1) + "-" + String.format(Locale.getDefault(), "%02d", dayOfMonth);
                etPurchaseDate.setText(date);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSavePurchase.setOnClickListener(v -> savePurchaseRecord());
        btnDeletePurchase.setOnClickListener(v -> deletePurchaseRecord());

        btnAddStock.setOnClickListener(v -> {
            if (SelectedStockManager.getInstance(this).isSelected(stock.getCode())) {
                SelectedStockManager.getInstance(this).removeStock(stock.getCode());
                Toast.makeText(this, "已取消自选 " + stock.getName(), Toast.LENGTH_SHORT).show();
            } else {
                SelectedStockManager.getInstance(this).addStock(stock);
                Toast.makeText(this, "已添加 " + stock.getName() + " 到自选", Toast.LENGTH_SHORT).show();
            }
            updateAddStockButton();
        });

        btnAiAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeepSeekChatActivity.class);
            intent.putExtra("code", stock.getCode());
            intent.putExtra("name", stock.getName());
            intent.putExtra("type", stock.getType());
            if (quote != null) {
                intent.putExtra("price", formatPrice(quote.getPrice()));
                double pct = quote.getPctChg();
                double chg = quote.getChange();
                intent.putExtra("change", (pct > 0 ? "+" : "") + formatPrice(chg) + " " + formatPct(pct) + "%");
            }
            startActivity(intent);
        });
    }

    private void loadPurchaseRecord() {
        currentRecord = purchaseRecordManager.getRecord(stock.getCode());
        if (currentRecord != null) {
            etPurchasePrice.setText(String.valueOf(currentRecord.getPrice()));
            etPurchaseDate.setText(currentRecord.getDate());
            purchaseInfoLayout.setVisibility(View.VISIBLE);
        } else {
            etPurchasePrice.setText("");
            etPurchaseDate.setText("");
            purchaseInfoLayout.setVisibility(View.GONE);
        }
    }

    private void savePurchaseRecord() {
        String priceStr = etPurchasePrice.getText().toString().trim();
        String dateStr = etPurchaseDate.getText().toString().trim();

        if (priceStr.isEmpty()) {
            etPurchasePrice.setError("请输入买入价格");
            return;
        }
        if (dateStr.isEmpty()) {
            etPurchaseDate.setError("请选择买入日期");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPurchasePrice.setError("价格格式不正确");
            return;
        }

        purchaseRecordManager.saveRecord(stock.getCode(), price, dateStr);
        currentRecord = purchaseRecordManager.getRecord(stock.getCode());
        purchaseInfoLayout.setVisibility(View.VISIBLE);
        updatePurchaseInfo();
    }

    private void deletePurchaseRecord() {
        purchaseRecordManager.deleteRecord(stock.getCode());
        currentRecord = null;
        etPurchasePrice.setText("");
        etPurchaseDate.setText("");
        purchaseInfoLayout.setVisibility(View.GONE);
    }

    private void updatePurchaseInfo() {
        if (currentRecord == null || quote == null) {
            return;
        }

        double costPrice = currentRecord.getPrice();
        double currentPrice = quote.getPrice();
        double profitLoss = currentPrice - costPrice;
        double profitLossPct = (profitLoss / costPrice) * 100;

        tvCostPrice.setText("成本价: " + formatPrice(costPrice) + "  日期: " + currentRecord.getDate());

        if (profitLoss > 0) {
            tvProfitLoss.setText(String.format(Locale.getDefault(), "盈亏: +%.3f  +%.3f%%", profitLoss, profitLossPct));
            tvProfitLoss.setTextColor(getResources().getColor(R.color.red));
        } else if (profitLoss < 0) {
            tvProfitLoss.setText(String.format(Locale.getDefault(), "盈亏: %.3f  %.3f%%", profitLoss, profitLossPct));
            tvProfitLoss.setTextColor(getResources().getColor(R.color.green));
        } else {
            tvProfitLoss.setText("盈亏: 0.000  0.000%");
            tvProfitLoss.setTextColor(Color.parseColor("#999999"));
        }
    }

    private void switchDays(int days) {
        try {
            isIntraday = (days == 0);
            currentDays = days;

            tabIntraday.setBackgroundResource(isIntraday ? R.drawable.chart_tab_bg : android.R.color.transparent);
            tabIntraday.setTextColor(isIntraday ? Color.BLACK : Color.parseColor("#666666"));
            tab5.setBackgroundResource(days == 5 ? R.drawable.chart_tab_bg : android.R.color.transparent);
            tab5.setTextColor(days == 5 ? Color.BLACK : Color.parseColor("#666666"));
            tab20.setBackgroundResource(days == 20 ? R.drawable.chart_tab_bg : android.R.color.transparent);
            tab20.setTextColor(days == 20 ? Color.BLACK : Color.parseColor("#666666"));

            if (isIntraday) {
                chartIntraday.setVisibility(View.VISIBLE);
                chartView.setVisibility(View.GONE);
                loadIntradayData();
                startIntradayRefresh();
            } else {
                chartIntraday.setVisibility(View.GONE);
                chartView.setVisibility(View.VISIBLE);
                stopIntradayRefresh();
                chartView.setDisplayDays(days);
                loadChartData(days);
            }
        } catch (Exception e) {
            Log.e("StockDetail", "switchDays error: " + e.getMessage(), e);
        }
    }

    private void loadData() {
        try {
            EastMoneyApi.fetchRealtime(stock.getCode(), q -> {
                try {
                    quote = q;
                    updateQuoteDisplay();
                } catch (Exception e) {
                    Log.e("StockDetail", "updateQuote error: " + e.getMessage(), e);
                }
            });

            EastMoneyApi.fetchExtra(stock.getCode(), q -> {
                try {
                    quote = q;
                    updateQuoteDisplay();
                } catch (Exception e) {
                    Log.e("StockDetail", "extra error: " + e.getMessage(), e);
                }
            });

            loadChartData(5);
            loadIntradayData();
            startIntradayRefresh();
        } catch (Exception e) {
            Log.e("StockDetail", "loadData error: " + e.getMessage(), e);
        }
    }

    private void loadIntradayData() {
        EastMoneyApi.fetchMinuteLine(stock.getCode(), data -> {
            if (data != null) {
                chartIntraday.setData(data);
            }
        });
    }

    private boolean isTradingHours() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return false;
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        return (hour == 9 && min >= 30) || (hour >= 10 && hour < 15) || (hour == 15 && min == 0);
    }

    private void startIntradayRefresh() {
        stopIntradayRefresh();
        if (!isIntraday) return;
        refreshTask = new Runnable() {
            @Override
            public void run() {
                if (isTradingHours()) {
                    loadIntradayData();
                    refreshHandler.postDelayed(this, 1000);
                } else if (isIntraday) {
                    refreshHandler.postDelayed(this, 3000);
                }
            }
        };
        refreshHandler.post(refreshTask);
    }

    private void stopIntradayRefresh() {
        if (refreshTask != null) {
            refreshHandler.removeCallbacks(refreshTask);
            refreshTask = null;
        }
    }

    private void updateAddStockButton() {
        if (SelectedStockManager.getInstance(this).isSelected(stock.getCode())) {
            btnAddStock.setText("已自选");
            btnAddStock.setBackgroundColor(0xFF888888);
        } else {
            btnAddStock.setText("+自选");
            btnAddStock.setBackgroundResource(R.drawable.btn_ai_bg);
        }
    }

    private void loadChartData(int days) {
        int klt = 5;
        int count = days * 48;
        EastMoneyApi.fetchKlineWithPeriod(stock.getCode(), count, klt, 1, data -> {
            try {
                chartView.setKlineData(data != null ? data : new ArrayList<>());
            } catch (Exception e) {
                Log.e("StockDetail", "chart error: " + e.getMessage(), e);
            }
        });
    }

    private void updateQuoteDisplay() {
        if (quote == null) return;

        priceText.setText(formatPrice(quote.getPrice()));
        double pct = quote.getPctChg();
        double chg = quote.getChange();
        if (pct > 0) {
            changeText.setText("+" + formatPrice(chg) + "  +" + formatPct(pct) + "%");
            changeText.setTextColor(getResources().getColor(R.color.red));
            priceText.setTextColor(getResources().getColor(R.color.red));
        } else if (pct < 0) {
            changeText.setText(formatPrice(chg) + "  " + formatPct(pct) + "%");
            changeText.setTextColor(getResources().getColor(R.color.green));
            priceText.setTextColor(getResources().getColor(R.color.green));
        } else {
            changeText.setText("0.00  0.00%");
            changeText.setTextColor(Color.parseColor("#999999"));
            priceText.setTextColor(Color.BLACK);
        }

        tvOpen.setText(formatPrice(quote.getOpen()));
        tvHigh.setText(formatPrice(quote.getHigh()));
        tvLow.setText(formatPrice(quote.getLow()));
        tvPreClose.setText(formatPrice(quote.getPreClose()));
        tvVolume.setText(formatVolume(quote.getVolume()));
        tvAmount.setText(formatAmount(quote.getAmount()));
        tvPe.setText(quote.getPe() > 0 ? formatNum(quote.getPe()) : "--");
        tvPb.setText(quote.getPb() > 0 ? formatNum(quote.getPb()) : "--");
        tvTurnover.setText(quote.getTurnoverRate() > 0 ? formatPct(quote.getTurnoverRate()) + "%" : "--");
        tvMarketcap.setText(formatMarketCap(quote.getTotalMarketCap()));

        if (stock.isEtf()) {
            etfExtraLabel.setVisibility(TextView.VISIBLE);
            tvIopv.setVisibility(TextView.VISIBLE);
            tvIopv.setText(quote.getIopv() > 0 ? formatPrice(quote.getIopv()) : "--");
            etfPremiumLabel.setVisibility(TextView.VISIBLE);
            tvPremium.setVisibility(TextView.VISIBLE);
            double pr = quote.getPremiumRate();
            if (pr != 0) {
                tvPremium.setText(formatPct(pr) + "%");
                tvPremium.setTextColor(pr > 0 ? getResources().getColor(R.color.red) : getResources().getColor(R.color.green));
            } else {
                tvPremium.setText("--");
            }
        } else {
            etfExtraLabel.setVisibility(TextView.GONE);
            tvIopv.setVisibility(TextView.GONE);
            etfPremiumLabel.setVisibility(TextView.GONE);
            tvPremium.setVisibility(TextView.GONE);
        }

        updatePurchaseInfo();
    }

    private String formatPrice(double v) {
        if (v == 0) return "--";
        return new DecimalFormat("#0.000").format(v);
    }

    private String formatPct(double v) {
        return new DecimalFormat("#0.000").format(v);
    }

    private String formatNum(double v) {
        return new DecimalFormat("#0.000").format(v);
    }

    private String formatVolume(double v) {
        if (v <= 0) return "--";
        if (v >= 10000) return new DecimalFormat("#0.00万手").format(v / 10000);
        return new DecimalFormat("#0手").format(v);
    }

    private String formatAmount(double v) {
        if (v <= 0) return "--";
        if (v >= 10000) return new DecimalFormat("#0.00亿").format(v / 10000);
        return new DecimalFormat("#0.00万").format(v);
    }

    private String formatMarketCap(double v) {
        if (v <= 0) return "--";
        double yi = v / 100000000.0;
        if (yi >= 10000) return new DecimalFormat("#0.000万亿").format(yi / 10000);
        return new DecimalFormat("#0.000亿").format(yi);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopIntradayRefresh();
    }
}
