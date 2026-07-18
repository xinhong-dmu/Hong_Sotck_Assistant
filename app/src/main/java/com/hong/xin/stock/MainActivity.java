package com.hong.xin.stock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.StockListCache;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.Stock;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SEARCH = 1;

    private SelectedStockManager stockManager;
    private SelectedStockAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stockManager = SelectedStockManager.getInstance(this);

        EastMoneyApi.init(StockListCache.getInstance(this));

        StrategyNotificationHelper.createChannel(this);
        StrategyAlarmScheduler.scheduleAlarms(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Stock> stocks = stockManager.getSelectedStocks();
        adapter = new SelectedStockAdapter(stocks, stock -> {
            stockManager.removeStock(stock.getCode());
            adapter.updateList(stockManager.getSelectedStocks());
            updateEmptyState();
        }, stock -> {
            Intent intent = new Intent(MainActivity.this, StockDetailActivity.class);
            intent.putExtra("code", stock.getCode());
            intent.putExtra("name", stock.getName());
            intent.putExtra("type", stock.getType());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        updateEmptyState();

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StockSearchActivity.class);
            startActivityForResult(intent, REQUEST_SEARCH);
        });

        TextView btnStrategies = findViewById(R.id.btn_strategies);
        btnStrategies.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StrategyListActivity.class);
            startActivity(intent);
        });
    }

    private void updateEmptyState() {
        TextView emptyView = findViewById(R.id.empty_view);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        List<Stock> stocks = stockManager.getSelectedStocks();
        if (stocks.isEmpty()) {
            emptyView.setVisibility(TextView.VISIBLE);
            recyclerView.setVisibility(RecyclerView.GONE);
        } else {
            emptyView.setVisibility(TextView.GONE);
            recyclerView.setVisibility(RecyclerView.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SEARCH && resultCode == RESULT_OK) {
            adapter.updateList(stockManager.getSelectedStocks());
            updateEmptyState();
        }
    }
}
