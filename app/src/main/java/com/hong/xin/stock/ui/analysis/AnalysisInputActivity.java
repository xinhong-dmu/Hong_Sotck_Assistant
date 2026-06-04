package com.hong.xin.stock.ui.analysis;

import android.os.Bundle;
import android.text.InputType;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.ui.widget.StockFilterAdapter;
import com.hong.xin.stock.util.SettingsManager;

public class AnalysisInputActivity extends AppCompatActivity {

    private AutoCompleteTextView searchEdit;
    private EditText stockCode, stockName, buyPriceAnalysis, buyDateAnalysis, klineDays;
    private EditText stopLossPercent, targetProfitPercent;
    private Button startAnalysisBtn;

    private StockRepository stockRepository;
    private StockFilterAdapter searchAdapter;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_input);

        settingsManager = new SettingsManager(this);
        stockRepository = StockRepository.getInstance();

        initViews();
        setupToolbar();
        setupAutoComplete();
        setupListeners();
        loadExistingParams();
    }

    private void initViews() {
        searchEdit = findViewById(R.id.search_edit);
        stockCode = findViewById(R.id.stock_code);
        stockName = findViewById(R.id.stock_name);
        buyPriceAnalysis = findViewById(R.id.buy_price_analysis);
        buyDateAnalysis = findViewById(R.id.buy_date_analysis);
        klineDays = findViewById(R.id.kline_days);
        stopLossPercent = findViewById(R.id.stop_loss_percent);
        targetProfitPercent = findViewById(R.id.target_profit_percent);
        startAnalysisBtn = findViewById(R.id.start_analysis_btn);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAutoComplete() {
        searchAdapter = new StockFilterAdapter(this);
        searchEdit.setThreshold(1);
        searchEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchEdit.setAdapter(searchAdapter);

        searchEdit.setOnItemClickListener((parent, view, position, id) -> {
            Stock selected = searchAdapter.getStock(position);
            if (selected != null) {
                searchEdit.dismissDropDown();
                stockName.setText(selected.getName());
                stockCode.setText(selected.getCode());
            }
            searchEdit.setText("");
        });
    }

    private void setupListeners() {
        stockCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateStockInfo();
        });
        stockName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateStockInfo();
        });

        klineDays.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = klineDays.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        int days = Integer.parseInt(text);
                        days = Math.max(5, Math.min(365, days));
                        settingsManager.setKlineDays(days);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });

        startAnalysisBtn.setOnClickListener(v -> startAnalysis());
    }

    private void loadExistingParams() {
        stockCode.setText(settingsManager.getLastStockCode());
        stockName.setText(settingsManager.getLastStockName());
        klineDays.setText(String.valueOf(settingsManager.getKlineDays()));

        double buyPrice = settingsManager.getLastBuyPrice();
        if (buyPrice > 0) {
            buyPriceAnalysis.setText(String.valueOf(buyPrice));
        }
        String buyDate = settingsManager.getLastBuyDate();
        if (!buyDate.isEmpty()) {
            buyDateAnalysis.setText(buyDate);
        }
        stopLossPercent.setText(String.valueOf(settingsManager.getStopLossPercent()));
        targetProfitPercent.setText(String.valueOf(settingsManager.getTargetProfitPercent()));
    }

    private void updateStockInfo() {
        String code = stockCode.getText().toString().trim();
        String name = stockName.getText().toString().trim();
        if (!code.isEmpty() && !name.isEmpty()) {
            settingsManager.setLastStockCode(code);
            settingsManager.setLastStockName(name);
        }
    }

    private void startAnalysis() {
        updateStockInfo();

        String code = stockCode.getText().toString().trim();
        String name = stockName.getText().toString().trim();
        if (code.isEmpty() || name.isEmpty()) {
            android.widget.Toast.makeText(this, "请先输入股票代码和名称", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String klineText = klineDays.getText().toString().trim();
        if (!klineText.isEmpty()) {
            try {
                int days = Integer.parseInt(klineText);
                days = Math.max(5, Math.min(365, days));
                settingsManager.setKlineDays(days);
            } catch (NumberFormatException ignored) {
            }
        }

        String priceText = buyPriceAnalysis.getText().toString().trim();
        if (!priceText.isEmpty()) {
            try {
                settingsManager.setLastBuyPrice(Double.parseDouble(priceText));
            } catch (NumberFormatException ignored) {
            }
        }

        String dateText = buyDateAnalysis.getText().toString().trim();
        if (!dateText.isEmpty()) {
            settingsManager.setLastBuyDate(dateText);
        }

        String stopLossText = stopLossPercent.getText().toString().trim();
        if (!stopLossText.isEmpty()) {
            try {
                settingsManager.setStopLossPercent(Double.parseDouble(stopLossText));
            } catch (NumberFormatException ignored) {
            }
        }

        String profitText = targetProfitPercent.getText().toString().trim();
        if (!profitText.isEmpty()) {
            try {
                settingsManager.setTargetProfitPercent(Double.parseDouble(profitText));
            } catch (NumberFormatException ignored) {
            }
        }

        settingsManager.setAnalysisRequested(true);
        setResult(RESULT_OK);
        finish();
    }
}
