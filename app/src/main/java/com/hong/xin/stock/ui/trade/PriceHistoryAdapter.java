package com.hong.xin.stock.ui.trade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.PriceRecord;

import java.util.List;
import java.util.Locale;

public class PriceHistoryAdapter extends RecyclerView.Adapter<PriceHistoryAdapter.ViewHolder> {

    public interface Callback {
        void onView(PriceRecord record);
        void onDelete(PriceRecord record);
    }

    private final List<PriceRecord> records;
    private final Callback callback;

    public PriceHistoryAdapter(List<PriceRecord> records, Callback callback) {
        this.records = records;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_price_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PriceRecord record = records.get(position);

        holder.stockText.setText(record.getStockName() + " (" + record.getStockCode() + ")");
        holder.priceText.setText(String.format(Locale.CHINA, "%.2f", record.getPrice()));

        double profit = record.getProfitPct();
        String profitStr = String.format(Locale.CHINA, "%.2f%%", profit);
        holder.profitText.setText(profitStr);
        holder.profitText.setTextColor(profit >= 0 ? 0xFFD32F2F : 0xFF4CAF50);

        holder.timeText.setText(record.getTime());
        holder.actionText.setText("操作: " + record.getAction());

        holder.viewBtn.setOnClickListener(v -> callback.onView(record));
        holder.deleteBtn.setOnClickListener(v -> callback.onDelete(record));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView stockText;
        final TextView priceText;
        final TextView profitText;
        final TextView timeText;
        final TextView actionText;
        final Button viewBtn;
        final Button deleteBtn;

        ViewHolder(View v) {
            super(v);
            stockText = v.findViewById(R.id.pitem_stock);
            priceText = v.findViewById(R.id.pitem_price);
            profitText = v.findViewById(R.id.pitem_profit);
            timeText = v.findViewById(R.id.pitem_time);
            actionText = v.findViewById(R.id.pitem_action);
            viewBtn = v.findViewById(R.id.pbtn_view);
            deleteBtn = v.findViewById(R.id.pbtn_delete);
        }
    }
}
