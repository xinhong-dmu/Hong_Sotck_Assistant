package com.hong.xin.stock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.Stock;

import java.util.List;

public class StockSearchActivity extends AppCompatActivity {

    private SelectedStockManager stockManager;
    private SearchResultAdapter adapter;
    private boolean hasChanged = false;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private TextView emptyHint;

    @Override
    public void onBackPressed() {
        if (hasChanged) {
            setResult(RESULT_OK);
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_search);

        stockManager = SelectedStockManager.getInstance(this);
        emptyHint = findViewById(R.id.empty_hint);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter(stock -> {
            boolean added = stockManager.addStock(stock);
            if (added) {
                hasChanged = true;
                Toast.makeText(this, "已添加: " + stock.getName(), Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "已在自选列表中", Toast.LENGTH_SHORT).show();
            }
        }, stock -> {
            Intent intent = new Intent(StockSearchActivity.this, StockDetailActivity.class);
            intent.putExtra("code", stock.getCode());
            intent.putExtra("name", stock.getName());
            intent.putExtra("type", stock.getType());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        setResult(RESULT_CANCELED);
        EastMoneyApi.ensureInit();
        EditText searchEdit = findViewById(R.id.search_edit);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s.toString().trim();
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    if (keyword.isEmpty()) {
                        adapter.updateList(null);
                        emptyHint.setVisibility(TextView.VISIBLE);
                        return;
                    }
                    emptyHint.setVisibility(TextView.GONE);
                    EastMoneyApi.searchSuggest(keyword, stocks -> {
                        adapter.updateList(stocks);
                        if (stocks.isEmpty()) {
                            emptyHint.setText("未找到匹配的股票");
                            emptyHint.setVisibility(TextView.VISIBLE);
                        }
                    });
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }
}
