package com.hong.xin.stock.ui.trade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.PriceRecord;
import com.hong.xin.stock.data.model.TradeRecord;
import com.hong.xin.stock.data.repository.PriceHistoryRepository;
import com.hong.xin.stock.data.repository.TradeRepository;

import java.util.List;

public class HistoryFragment extends Fragment {

    private Button tabTradeRecords, tabPriceHistory;
    private TextView emptyText, titleText;
    private RecyclerView historyRecycler;

    private HistoryAdapter tradeAdapter;
    private PriceHistoryAdapter priceAdapter;
    private List<TradeRecord> tradeRecords;
    private List<PriceRecord> priceRecords;
    private boolean showingTradeRecords = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews(View view) {
        tabTradeRecords = view.findViewById(R.id.tab_trade_records);
        tabPriceHistory = view.findViewById(R.id.tab_price_history);
        emptyText = view.findViewById(R.id.empty_text);
        titleText = view.findViewById(R.id.title_text);
        historyRecycler = view.findViewById(R.id.history_recycler);

        historyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupListeners() {
        tabTradeRecords.setOnClickListener(v -> {
            showingTradeRecords = true;
            updateTabStyles();
            showCurrentData();
        });

        tabPriceHistory.setOnClickListener(v -> {
            showingTradeRecords = false;
            updateTabStyles();
            showCurrentData();
        });
    }

    private void updateTabStyles() {
        if (showingTradeRecords) {
            tabTradeRecords.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC62828));
            tabTradeRecords.setTextColor(0xFFFFFFFF);
            tabPriceHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
            tabPriceHistory.setTextColor(0xFF000000);
        } else {
            tabPriceHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC62828));
            tabPriceHistory.setTextColor(0xFFFFFFFF);
            tabTradeRecords.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
            tabTradeRecords.setTextColor(0xFF000000);
        }
    }

    private void loadData() {
        tradeRecords = TradeRepository.readRecords(requireContext().getCacheDir());
        priceRecords = PriceHistoryRepository.readAll(requireContext().getCacheDir());

        tradeAdapter = new HistoryAdapter(tradeRecords, new HistoryAdapter.Callback() {
            @Override
            public void onView(TradeRecord record) {
                String info = String.format("股票: %s(%s)\n买入价: %.2f\n最高价: %.2f\n退出价: %.2f\n盈亏: %.2f%%\n原因: %s",
                        record.getStockName(), record.getStockCode(),
                        record.getBuyPrice(), record.getHighestPrice(),
                        record.getExitPrice(), record.getProfitPct(), record.getReason());
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("交易详情")
                        .setMessage(info)
                        .setPositiveButton("确定", null)
                        .show();
            }

            @Override
            public void onDelete(TradeRecord record) {
                Toast.makeText(requireContext(), "暂不支持删除交易记录", Toast.LENGTH_SHORT).show();
            }
        });

        priceAdapter = new PriceHistoryAdapter(priceRecords, new PriceHistoryAdapter.Callback() {
            @Override
            public void onView(PriceRecord record) {
                String info = String.format("股票: %s(%s)\n价格: %.2f\n买入价: %.2f\n最高价: %.2f\n防守线: %.2f\n止损线: %.2f\n盈亏: %.2f%%\n回撤: %.2f%%\n操作: %s",
                        record.getStockName(), record.getStockCode(),
                        record.getPrice(), record.getBuyPrice(),
                        record.getHighestPrice(), record.getDefenseLine(),
                        record.getHardStopLine(), record.getProfitPct(),
                        record.getDrawdown(), record.getAction());
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("价格历史详情")
                        .setMessage(info)
                        .setPositiveButton("确定", null)
                        .show();
            }

            @Override
            public void onDelete(PriceRecord record) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除这条记录吗?")
                        .setPositiveButton("确定", (dialog, which) -> {
                            PriceHistoryRepository.deleteRecord(requireContext().getCacheDir(), record);
                            priceRecords.remove(record);
                            priceAdapter.notifyDataSetChanged();
                            if (priceRecords.isEmpty()) showCurrentData();
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        showCurrentData();
    }

    private void showCurrentData() {
        if (showingTradeRecords) {
            titleText.setText("交易记录");
            if (tradeRecords.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                historyRecycler.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                historyRecycler.setVisibility(View.VISIBLE);
                historyRecycler.setAdapter(tradeAdapter);
            }
        } else {
            titleText.setText("价格历史");
            if (priceRecords.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                historyRecycler.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                historyRecycler.setVisibility(View.VISIBLE);
                historyRecycler.setAdapter(priceAdapter);
            }
        }
    }
}
