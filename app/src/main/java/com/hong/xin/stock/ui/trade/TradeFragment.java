package com.hong.xin.stock.ui.trade;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.ui.widget.StockFilterAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TradeFragment extends Fragment {

    private TradeViewModel viewModel;

    private AutoCompleteTextView searchEdit;
    private EditText stockName, stockCode, buyPrice, buyDate, stopLossPercent, targetProfitPercent;
    private EditText trailingPercent, currentPrice;
    private CheckBox useGraded;
    private Button updatePriceBtn, confirmPriceBtn, clearTradeBtn;
    private View dashboardCard;
    private TextView dashboardHighest, dashboardDefense, dashboardHardStop, dashboardProfit;
    private TextView dashboardDrawdown, dashboardDistance, dashboardTrailingNote;

    private StockRepository stockRepository;
    private StockFilterAdapter searchAdapter;
    private boolean updatingFromObserver = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TradeViewModel.class);
        stockRepository = StockRepository.getInstance();

        initViews(view);
        setupAutoComplete();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        searchEdit = view.findViewById(R.id.search_edit);
        stockName = view.findViewById(R.id.stock_name);
        stockCode = view.findViewById(R.id.stock_code);
        buyPrice = view.findViewById(R.id.buy_price);
        buyDate = view.findViewById(R.id.buy_date);
        stopLossPercent = view.findViewById(R.id.stop_loss_percent);
        targetProfitPercent = view.findViewById(R.id.target_profit_percent);
        trailingPercent = view.findViewById(R.id.trailing_percent);
        useGraded = view.findViewById(R.id.use_graded);
        currentPrice = view.findViewById(R.id.current_price);
        updatePriceBtn = view.findViewById(R.id.update_price_btn);
        confirmPriceBtn = view.findViewById(R.id.confirm_price_btn);
        clearTradeBtn = view.findViewById(R.id.clear_trade_btn);
        dashboardCard = view.findViewById(R.id.dashboard_card);
        dashboardHighest = view.findViewById(R.id.dashboard_highest);
        dashboardDefense = view.findViewById(R.id.dashboard_defense);
        dashboardHardStop = view.findViewById(R.id.dashboard_hard_stop);
        dashboardProfit = view.findViewById(R.id.dashboard_profit);
        dashboardDrawdown = view.findViewById(R.id.dashboard_drawdown);
        dashboardDistance = view.findViewById(R.id.dashboard_distance);
        dashboardTrailingNote = view.findViewById(R.id.dashboard_trailing_note);
    }

    private void setupAutoComplete() {
        searchAdapter = new StockFilterAdapter(requireContext());
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
        buyDate.setOnClickListener(v -> showDatePicker());

        useGraded.setOnCheckedChangeListener((buttonView, isChecked) -> {
            trailingPercent.setEnabled(isChecked);
            if (!isChecked) {
                trailingPercent.setText("0");
            }
        });

        updatePriceBtn.setOnClickListener(v -> viewModel.fetchRealtimePrice());

        confirmPriceBtn.setOnClickListener(v -> {
            String priceStr = currentPrice.getText().toString().trim();
            if (priceStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入当前股价", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double price = Double.parseDouble(priceStr);
                saveInputParams();
                viewModel.startStrategy(price);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "请输入有效股价", Toast.LENGTH_SHORT).show();
            }
        });

        clearTradeBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("确认重置")
                    .setMessage("确定要结束/重置当前交易吗?")
                    .setPositiveButton("确定", (dialog, which) -> viewModel.resetTrade())
                    .setNegativeButton("取消", null)
                    .show();
        });

        stockCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (updatingFromObserver) return;
                String code = s.toString().trim();
                String name = stockName.getText().toString().trim();
                if (!code.isEmpty() && !name.isEmpty()) {
                    viewModel.updateStockInfo(name, code);
                }
            }
        });

        currentPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String priceStr = currentPrice.getText().toString().trim();
                if (!priceStr.isEmpty()) {
                    try {
                        double price = Double.parseDouble(priceStr);
                        viewModel.updateCurrentPrice(price);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            updatingFromObserver = true;
            stockName.setText(state.getStockName());
            stockCode.setText(state.getStockCode());
            updatingFromObserver = false;
            if (state.getBuyPrice() > 0) buyPrice.setText(String.valueOf(state.getBuyPrice()));
            buyDate.setText(state.getBuyDate());
            stopLossPercent.setText(String.valueOf((int) state.getStopLossPercent()));
            targetProfitPercent.setText(String.valueOf((int) state.getTargetProfitPercent()));
            trailingPercent.setText(String.valueOf((int) state.getTrailingPercent()));
            useGraded.setChecked(state.isUseGraded());
            trailingPercent.setEnabled(state.isUseGraded());
            if (state.getCurrentPrice() > 0) currentPrice.setText(String.format("%.2f", state.getCurrentPrice()));

            if (state.isDashboardVisible()) {
                dashboardCard.setVisibility(View.VISIBLE);
                updateDashboard(state);
            } else {
                dashboardCard.setVisibility(View.GONE);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                viewModel.clearToastMessage();
            }
        });

        viewModel.getAlertDialog().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("交易提醒")
                        .setMessage(msg)
                        .setPositiveButton("确定", (dialog, which) -> viewModel.clearAlertDialog())
                        .show();
            }
        });
    }

    private void updateDashboard(TradeUiState state) {
        dashboardHighest.setText("📈 买入以来最高价: " + String.format("%.2f", state.getHighestPrice()));
        dashboardDefense.setText("🛡 当前防守线: " + String.format("%.2f", state.getDefenseLine()));
        dashboardHardStop.setText("⚠ 绝对止损线: " + String.format("%.2f", state.getHardStopLine()));

        double profit = state.getProfitPct();
        String profitStr = String.format("💰 当前账面收益: %.2f%%", profit);
        if (profit >= 0) {
            dashboardProfit.setTextColor(0xFFD32F2F);
            dashboardProfit.setText(profitStr + " 📈");
        } else {
            dashboardProfit.setTextColor(0xFF4CAF50);
            dashboardProfit.setText(profitStr + " 📉");
        }

        double drawdown = state.getDrawdown();
        String ddStr = String.format("📉 距最高点回撤: %.2f%%", drawdown);
        if (drawdown > 5) {
            dashboardDrawdown.setTextColor(0xFFD32F2F);
            dashboardDrawdown.setText(ddStr + " ⚠");
        } else {
            dashboardDrawdown.setTextColor(0xFF333333);
            dashboardDrawdown.setText(ddStr);
        }

        double dist = state.getDistanceToDefense();
        String distStr = String.format("📊 距防守线空间: %.2f%%", dist);
        dashboardDistance.setText(dist < 0 ? distStr + " ⚠⚠⚠" : distStr);

        String note = "⚙ 有效回撤容忍: " + String.format("%.1f%%", state.getEffectiveTrailing());
        if (state.isGradedEffect()) {
            note += " | 分级收紧已启用";
        } else {
            note += " | 分级收紧未启用";
        }
        dashboardTrailingNote.setText(note);

        if (state.getHighestPrice() > 0) {
            dashboardHighest.setTextColor(0xFFE65100);
        }
        if (state.getDefenseLine() > 0) {
            dashboardDefense.setTextColor(0xFF1565C0);
        }
        if (state.getHardStopLine() > 0) {
            dashboardHardStop.setTextColor(0xFFD32F2F);
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        String dateStr = buyDate.getText().toString().trim();
        if (!dateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                cal.setTime(sdf.parse(dateStr));
            } catch (Exception ignored) {
            }
        }
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            buyDate.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveInputParams() {
        String name = stockName.getText().toString().trim();
        String code = stockCode.getText().toString().trim();
        double buyP = 0;
        try { buyP = Double.parseDouble(buyPrice.getText().toString().trim()); } catch (NumberFormatException ignored) {}
        String date = buyDate.getText().toString().trim();
        double stopLoss = 3;
        try { stopLoss = Double.parseDouble(stopLossPercent.getText().toString().trim()); } catch (NumberFormatException ignored) {}
        double targetProfit = 10;
        try { targetProfit = Double.parseDouble(targetProfitPercent.getText().toString().trim()); } catch (NumberFormatException ignored) {}
        double trailing = 0;
        try { trailing = Double.parseDouble(trailingPercent.getText().toString().trim()); } catch (NumberFormatException ignored) {}
        boolean graded = useGraded.isChecked();

        viewModel.saveTradeParams(name, code, buyP, date, stopLoss, targetProfit, trailing, graded);
    }
}
