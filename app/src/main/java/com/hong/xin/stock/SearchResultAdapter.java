package com.hong.xin.stock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.model.Stock;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<Stock> stocks = new ArrayList<>();
    private final OnStockClickListener onStockClick;
    private final OnItemClickListener onItemClick;

    interface OnStockClickListener {
        void onStockClick(Stock stock);
    }

    interface OnItemClickListener {
        void onItemClick(Stock stock);
    }

    SearchResultAdapter(OnStockClickListener onStockClick, OnItemClickListener onItemClick) {
        this.onStockClick = onStockClick;
        this.onItemClick = onItemClick;
    }

    void updateList(List<Stock> newStocks) {
        this.stocks = newStocks != null ? newStocks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.codeText.setText(stock.getCode());
        holder.nameText.setText(stock.getName());
        holder.addBtn.setOnClickListener(v -> onStockClick.onStockClick(stock));
        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onItemClick(stock);
        });
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView codeText, nameText, addBtn;

        ViewHolder(View itemView) {
            super(itemView);
            codeText = itemView.findViewById(R.id.stock_code);
            nameText = itemView.findViewById(R.id.stock_name);
            addBtn = itemView.findViewById(R.id.btn_add);
        }
    }
}
