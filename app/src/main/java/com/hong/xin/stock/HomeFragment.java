package com.hong.xin.stock;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.PurchaseRecordManager;
import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.model.PurchaseRecord;
import com.hong.xin.stock.data.model.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private SelectedStockManager stockManager;
    private PurchaseRecordManager purchaseRecordManager;

    private SelectedStockAdapter watchlistAdapter;
    private PositionHoldingAdapter positionAdapter;

    private RecyclerView recyclerView;
    private View emptyView;
    private TextView emptyText, emptyHint;
    private View positionEmptyView;

    private TextView tabWatchlist, tabPosition;
    private boolean showingPosition = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        stockManager = SelectedStockManager.getInstance(requireContext());
        purchaseRecordManager = PurchaseRecordManager.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        emptyView = view.findViewById(R.id.empty_view);
        emptyText = view.findViewById(R.id.empty_text);
        emptyHint = view.findViewById(R.id.empty_hint);
        positionEmptyView = view.findViewById(R.id.position_empty_view);

        tabWatchlist = view.findViewById(R.id.tab_watchlist);
        tabPosition = view.findViewById(R.id.tab_position);

        watchlistAdapter = new SelectedStockAdapter(stockManager.getSelectedStocks(),
                stock -> {
                    stockManager.removeStock(stock.getCode());
                    watchlistAdapter.updateList(stockManager.getSelectedStocks());
                    updateEmptyState();
                },
                stock -> openStockDetail(stock));

        positionAdapter = new PositionHoldingAdapter(
                new ArrayList<>(),
                new HashMap<>(),
                stockCode -> {
                    purchaseRecordManager.deleteAllRecords(stockCode);
                    refreshPositionList();
                },
                stock -> openStockDetail(stock));

        tabWatchlist.setOnClickListener(v -> switchTab(false));
        tabPosition.setOnClickListener(v -> switchTab(true));

        switchTab(false);
        return view;
    }

    private void switchTab(boolean showPosition) {
        showingPosition = showPosition;

        if (showPosition) {
            tabWatchlist.setBackgroundResource(R.drawable.bg_outlined_button);
            tabWatchlist.setTextColor(getResources().getColor(R.color.primary, null));
            tabPosition.setBackgroundResource(R.drawable.bg_primary_button);
            tabPosition.setTextColor(getResources().getColor(R.color.white, null));
            recyclerView.setAdapter(positionAdapter);
            refreshPositionList();
            emptyView.setVisibility(View.GONE);
        } else {
            tabWatchlist.setBackgroundResource(R.drawable.bg_primary_button);
            tabWatchlist.setTextColor(getResources().getColor(R.color.white, null));
            tabPosition.setBackgroundResource(R.drawable.bg_outlined_button);
            tabPosition.setTextColor(getResources().getColor(R.color.primary, null));
            recyclerView.setAdapter(watchlistAdapter);
            positionEmptyView.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void refreshPositionList() {
        Map<String, List<PurchaseRecord>> allRecords = purchaseRecordManager.getAllRecords();
        List<Map.Entry<String, List<PurchaseRecord>>> validEntries = new ArrayList<>();

        Map<String, Stock> stockMap = new HashMap<>();
        for (Stock s : stockManager.getSelectedStocks()) {
            stockMap.put(s.getCode(), s);
        }

        for (Map.Entry<String, List<PurchaseRecord>> entry : allRecords.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                validEntries.add(entry);
                if (!stockMap.containsKey(entry.getKey())) {
                    stockMap.put(entry.getKey(), new Stock(entry.getKey(), entry.getKey(), Stock.TYPE_STOCK));
                }
            }
        }

        positionAdapter.updateData(validEntries, stockMap);

        if (validEntries.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            positionEmptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            positionEmptyView.setVisibility(View.GONE);
        }
    }

    private void openStockDetail(Stock stock) {
        Intent intent = new Intent(requireActivity(), StockDetailActivity.class);
        intent.putExtra("code", stock.getCode());
        intent.putExtra("name", stock.getName());
        intent.putExtra("type", stock.getType());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (showingPosition) {
            refreshPositionList();
        } else {
            if (watchlistAdapter != null) {
                watchlistAdapter.updateList(stockManager.getSelectedStocks());
                updateEmptyState();
            }
        }
    }

    public void refreshList() {
        if (showingPosition) {
            refreshPositionList();
        } else {
            if (watchlistAdapter != null) {
                watchlistAdapter.updateList(stockManager.getSelectedStocks());
                updateEmptyState();
            }
        }
    }

    private void updateEmptyState() {
        List<Stock> stocks = stockManager.getSelectedStocks();
        positionEmptyView.setVisibility(View.GONE);

        if (stocks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyText.setText("暂无自选股");
            emptyHint.setText("点击右下角 + 添加");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
