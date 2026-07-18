package com.hong.xin.stock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.model.Stock;

import java.util.List;

public class SelectedStockAdapter extends RecyclerView.Adapter<SelectedStockAdapter.ViewHolder> {

    private List<Stock> stocks;
    private final OnDeleteClickListener onDeleteClick;
    private final OnItemClickListener onItemClick;

    interface OnDeleteClickListener {
        void onDeleteClick(Stock stock);
    }

    interface OnItemClickListener {
        void onItemClick(Stock stock);
    }

    SelectedStockAdapter(List<Stock> stocks, OnDeleteClickListener onDeleteClick,
                         OnItemClickListener onItemClick) {
        this.stocks = stocks;
        this.onDeleteClick = onDeleteClick;
        this.onItemClick = onItemClick;
    }

    void updateList(List<Stock> newStocks) {
        this.stocks = newStocks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.codeText.setText(stock.getCode());
        holder.nameText.setText(stock.getName());
        holder.deleteBtn.setOnClickListener(v -> onDeleteClick.onDeleteClick(stock));
        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onItemClick(stock);
        });
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView codeText, nameText, deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);
            codeText = itemView.findViewById(R.id.stock_code);
            nameText = itemView.findViewById(R.id.stock_name);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
        }
    }
}
