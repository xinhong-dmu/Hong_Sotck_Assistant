package com.hong.xin.stock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.PurchaseRecord;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Stock;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class PositionHoldingAdapter extends RecyclerView.Adapter<PositionHoldingAdapter.ViewHolder> {

    private List<Map.Entry<String, List<PurchaseRecord>>> entries;
    private Map<String, Stock> stockMap;
    private final OnDeleteListener onDeleteListener;
    private final OnItemClickListener onItemClickListener;

    interface OnDeleteListener {
        void onDelete(String stockCode);
    }

    interface OnItemClickListener {
        void onItemClick(Stock stock);
    }

    PositionHoldingAdapter(List<Map.Entry<String, List<PurchaseRecord>>> entries,
                           Map<String, Stock> stockMap,
                           OnDeleteListener onDeleteListener,
                           OnItemClickListener onItemClickListener) {
        this.entries = entries;
        this.stockMap = stockMap;
        this.onDeleteListener = onDeleteListener;
        this.onItemClickListener = onItemClickListener;
    }

    void updateData(List<Map.Entry<String, List<PurchaseRecord>>> newEntries,
                     Map<String, Stock> newStockMap) {
        this.entries = newEntries;
        this.stockMap = newStockMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_position_holding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, List<PurchaseRecord>> entry = entries.get(position);
        String code = entry.getKey();
        List<PurchaseRecord> records = entry.getValue();

        Stock stock = stockMap.get(code);
        String name = stock != null ? stock.getName() : code;
        String type = stock != null ? stock.getType() : Stock.TYPE_STOCK;

        holder.tvCode.setText(code);
        holder.tvName.setText(name);

        double sum = 0;
        for (PurchaseRecord r : records) {
            sum += r.getPrice();
        }
        final double avgCost = sum / records.size();

        DecimalFormat priceDf = new DecimalFormat("#0.000");
        DecimalFormat pnlDf = new DecimalFormat("+0.00%;-0.00%");

        holder.tvCost.setText("成本 " + priceDf.format(avgCost));
        holder.tvPrice.setText("加载中...");
        holder.tvPnl.setText("--");
        holder.tvPnl.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint, null));

        holder.tvDelete.setOnClickListener(v -> {
            if (onDeleteListener != null) onDeleteListener.onDelete(code);
        });

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null && stock != null) {
                onItemClickListener.onItemClick(stock);
            }
        });

        holder.itemView.setTag(code);

        EastMoneyApi.fetchRealtime(code, quote -> {
            if (!code.equals(holder.itemView.getTag())) return;

            double currentPrice = quote.getPrice();
            if (currentPrice > 0) {
                holder.tvPrice.setText(priceDf.format(currentPrice));
                double pnl = (currentPrice - avgCost) / avgCost;
                holder.tvPnl.setText(pnlDf.format(pnl));
                if (pnl > 0) {
                    holder.tvPnl.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.stock_up, null));
                } else if (pnl < 0) {
                    holder.tvPnl.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.stock_down, null));
                } else {
                    holder.tvPnl.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint, null));
                }
            }
        });

        holder.llRecords.removeAllViews();
        for (PurchaseRecord r : records) {
            TextView tv = new TextView(holder.itemView.getContext());
            tv.setText("买入价 " + priceDf.format(r.getPrice()) + "  " + r.getDate());
            tv.setTextSize(12);
            tv.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary, null));
            tv.setPadding(0, 1, 0, 1);
            holder.llRecords.addView(tv);
        }

        if (records.size() > 1) {
            TextView tv = new TextView(holder.itemView.getContext());
            tv.setText("均价 " + priceDf.format(avgCost) + "  (共" + records.size() + "笔)");
            tv.setTextSize(11);
            tv.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint, null));
            tv.setPadding(0, 2, 0, 0);
            holder.llRecords.addView(tv);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvCost, tvPrice, tvPnl, tvDelete;
        LinearLayout llRecords;

        ViewHolder(View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_code);
            tvName = itemView.findViewById(R.id.tv_name);
            tvCost = itemView.findViewById(R.id.tv_cost);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvPnl = itemView.findViewById(R.id.tv_pnl);
            tvDelete = itemView.findViewById(R.id.tv_delete);
            llRecords = itemView.findViewById(R.id.ll_records);
        }
    }
}
