package com.hong.xin.stock;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

    private TextView btnAddStock, btnAddTradeRecord, btnAiAnalysisHeader;
    private LinearLayout purchaseInfoLayout;
    private LinearLayout purchaseRecordsContainer;
    private TextView tvAvgCost, tvProfitLoss;

    private PurchaseRecordManager purchaseRecordManager;

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

        btnAddStock = findViewById(R.id.btn_add_stock);
        btnAddTradeRecord = findViewById(R.id.btn_add_trade_record);
        btnAiAnalysisHeader = findViewById(R.id.btn_ai_analysis_header);
        purchaseInfoLayout = findViewById(R.id.purchase_info_layout);
        purchaseRecordsContainer = findViewById(R.id.purchase_records_container);
        tvAvgCost = findViewById(R.id.tv_avg_cost);
        tvProfitLoss = findViewById(R.id.tv_profit_loss);

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

        btnAddTradeRecord.setOnClickListener(v -> showAddTradeRecordDialog());

        btnAiAnalysisHeader.setOnClickListener(v -> {
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

        refreshPurchaseRecords();
    }

    private void showAddTradeRecordDialog() {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(40, 20, 40, 20);

        EditText etPrice = new EditText(this);
        etPrice.setHint("输入价格");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setTextSize(18);
        etPrice.setPadding(12, 12, 12, 12);
        etPrice.setMinHeight(56);

        EditText etDate = new EditText(this);
        etDate.setHint("点击选择日期");
        etDate.setFocusable(false);
        etDate.setCursorVisible(false);
        etDate.setTextSize(18);
        etDate.setPadding(12, 12, 12, 12);
        etDate.setMinHeight(56);

        Calendar cal = Calendar.getInstance();
        String today = cal.get(Calendar.YEAR) + "-" +
                String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1) + "-" +
                String.format(Locale.getDefault(), "%02d", cal.get(Calendar.DAY_OF_MONTH));
        etDate.setText(today);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String date = year + "-" + String.format(Locale.getDefault(), "%02d", month + 1) + "-"
                        + String.format(Locale.getDefault(), "%02d", dayOfMonth);
                etDate.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogLayout.addView(etPrice);
        dialogLayout.addView(etDate);

        new AlertDialog.Builder(this)
                .setTitle("添加买卖记录")
                .setView(dialogLayout)
                .setPositiveButton("确定", (dialog, which) -> {
                    String priceStr = etPrice.getText().toString().trim();
                    String dateStr = etDate.getText().toString().trim();

                    if (priceStr.isEmpty()) {
                        Toast.makeText(this, "请输入价格", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "价格格式不正确", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    purchaseRecordManager.addRecord(stock.getCode(), price, dateStr);
                    refreshPurchaseRecords();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void refreshPurchaseRecords() {
        purchaseRecordsContainer.removeAllViews();
        List<PurchaseRecord> list = purchaseRecordManager.getRecords(stock.getCode());

        if (list.isEmpty()) {
            purchaseInfoLayout.setVisibility(View.GONE);
            purchaseRecordsContainer.setVisibility(View.GONE);
            return;
        }

        purchaseRecordsContainer.setVisibility(View.VISIBLE);
        purchaseInfoLayout.setVisibility(View.VISIBLE);

        DecimalFormat df = new DecimalFormat("#0.000");

        double totalCost = 0;
        for (PurchaseRecord r : list) totalCost += r.getPrice();
        double avgCost = totalCost / list.size();

        tvAvgCost.setText("均价: " + df.format(avgCost) + "  共" + list.size() + "笔");

        for (int i = 0; i < list.size(); i++) {
            PurchaseRecord record = list.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 3, 0, 3);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvIdx = new TextView(this);
            tvIdx.setText((i + 1) + ".");
            tvIdx.setTextSize(13);
            tvIdx.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvIdx.setPadding(0, 0, 6, 0);
            row.addView(tvIdx);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(df.format(record.getPrice()) + "  " + record.getDate());
            tvInfo.setTextSize(13);
            tvInfo.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(tvInfo);

            if (quote != null && quote.getPrice() > 0) {
                double pnl = quote.getPrice() - record.getPrice();
                double pnlPct = (pnl / record.getPrice()) * 100;
                TextView tvPnl = new TextView(this);
                tvPnl.setText(String.format(Locale.getDefault(), "%+.2f%%", pnlPct));
                tvPnl.setTextSize(12);
                tvPnl.setTextColor(pnlPct >= 0 ?
                        getResources().getColor(R.color.red) :
                        getResources().getColor(R.color.green));
                tvPnl.setPadding(8, 0, 8, 0);
                row.addView(tvPnl);
            }

            TextView btnDel = new TextView(this);
            btnDel.setText("删除");
            btnDel.setTextSize(11);
            btnDel.setTextColor(0xFFFF0000);
            btnDel.setPadding(12, 4, 12, 4);
            btnDel.setBackgroundResource(R.drawable.bg_chip);
            String recordId = record.getId();
            btnDel.setOnClickListener(v -> {
                new AlertDialog.Builder(StockDetailActivity.this)
                        .setTitle("确认删除")
                        .setMessage("删除这笔买入记录：" + df.format(record.getPrice()) + " " + record.getDate() + "？")
                        .setPositiveButton("确定", (d, w) -> {
                            purchaseRecordManager.deleteRecord(stock.getCode(), recordId);
                            refreshPurchaseRecords();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
            row.addView(btnDel);

            purchaseRecordsContainer.addView(row);
        }

        updatePurchaseInfo(avgCost);
        updateChartPurchaseLines();
    }

    private void updatePurchaseInfo(double avgCost) {
        if (quote == null) return;

        double currentPrice = quote.getPrice();
        double profitLoss = currentPrice - avgCost;
        double profitLossPct = (profitLoss / avgCost) * 100;

        if (profitLoss > 0) {
            tvProfitLoss.setText(String.format(Locale.getDefault(), "浮动盈亏: +%.3f  +%.3f%%", profitLoss, profitLossPct));
            tvProfitLoss.setTextColor(getResources().getColor(R.color.red));
        } else if (profitLoss < 0) {
            tvProfitLoss.setText(String.format(Locale.getDefault(), "浮动盈亏: %.3f  %.3f%%", profitLoss, profitLossPct));
            tvProfitLoss.setTextColor(getResources().getColor(R.color.green));
        } else {
            tvProfitLoss.setText("浮动盈亏: 0.000  0.000%");
            tvProfitLoss.setTextColor(getResources().getColor(R.color.text_secondary, null));
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
                    if (quote == null) {
                        quote = q;
                    } else {
                        quote = new RealtimeQuote.Builder(quote)
                                .pe(q.getPe())
                                .pb(q.getPb())
                                .turnoverRate(q.getTurnoverRate())
                                .volumeRatio(q.getVolumeRatio())
                                .totalMarketCap(q.getTotalMarketCap())
                                .circulatingMarketCap(q.getCirculatingMarketCap())
                                .limitUp(q.getLimitUp())
                                .limitDown(q.getLimitDown())
                                .eps(q.getEps())
                                .dividendYield(q.getDividendYield())
                                .ma5(q.getMa5())
                                .ma10(q.getMa10())
                                .ma20(q.getMa20())
                                .ma30(q.getMa30())
                                .ma60(q.getMa60())
                                .iopv(q.getIopv())
                                .premiumRate(q.getPremiumRate())
                                .build();
                    }
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

    private void updateChartPurchaseLines() {
        List<PurchaseRecord> records = purchaseRecordManager.getRecords(stock.getCode());
        List<Double> prices = new ArrayList<>();
        for (PurchaseRecord r : records) {
            prices.add(r.getPrice());
        }
        chartIntraday.setPurchasePrices(prices);
        chartView.setPurchasePrices(prices);
    }

    private void updateAddStockButton() {
        if (SelectedStockManager.getInstance(this).isSelected(stock.getCode())) {
            btnAddStock.setText("已自选");
            btnAddStock.setBackgroundResource(R.drawable.bg_chip);
        } else {
            btnAddStock.setText("+自选");
            btnAddStock.setBackgroundResource(R.drawable.bg_primary_button);
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
            changeText.setTextColor(getResources().getColor(R.color.text_secondary, null));
            priceText.setTextColor(getResources().getColor(R.color.text_primary, null));
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
            etfPremiumLabel.setVisibility(TextView.VISIBLE);
            tvPremium.setVisibility(TextView.VISIBLE);
            double iopv = quote.getIopv();
            double pr = quote.getPremiumRate();
            if (iopv > 0) {
                tvIopv.setText(formatPrice(iopv));
                tvPremium.setText(formatPct(pr) + "%");
                tvPremium.setTextColor(pr > 0 ? getResources().getColor(R.color.red)
                        : (pr < 0 ? getResources().getColor(R.color.green) : getResources().getColor(R.color.text_secondary, null)));
            } else {
                tvIopv.setText("--");
                tvPremium.setText("--");
            }
        } else {
            etfExtraLabel.setVisibility(TextView.GONE);
            tvIopv.setVisibility(TextView.GONE);
            etfPremiumLabel.setVisibility(TextView.GONE);
            tvPremium.setVisibility(TextView.GONE);
        }

        refreshPurchaseRecords();
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
