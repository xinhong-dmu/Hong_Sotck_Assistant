package com.hong.xin.stock.ui.trade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.TradeRecord;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface Callback {
        void onView(TradeRecord record);
        void onDelete(TradeRecord record);
    }

    private final List<TradeRecord> records;
    private final Callback callback;

    public HistoryAdapter(List<TradeRecord> records, Callback callback) {
        this.records = records;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trade_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TradeRecord record = records.get(position);

        holder.stockText.setText(record.getStockName() + " (" + record.getStockCode() + ")");

        double profit = record.getProfitPct();
        String profitStr = String.format(Locale.CHINA, "%.2f%%", profit);
        holder.profitText.setText(profitStr);
        holder.profitText.setTextColor(profit >= 0 ? 0xFFD32F2F : 0xFF4CAF50);

        holder.timeText.setText(record.getTime());
        holder.reasonText.setText("原因: " + record.getReason());

        holder.viewBtn.setOnClickListener(v -> callback.onView(record));
        holder.deleteBtn.setOnClickListener(v -> callback.onDelete(record));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView stockText;
        final TextView profitText;
        final TextView timeText;
        final TextView reasonText;
        final Button viewBtn;
        final Button deleteBtn;

        ViewHolder(View v) {
            super(v);
            stockText = v.findViewById(R.id.item_stock);
            profitText = v.findViewById(R.id.item_profit);
            timeText = v.findViewById(R.id.item_time);
            reasonText = v.findViewById(R.id.item_reason);
            viewBtn = v.findViewById(R.id.btn_view);
            deleteBtn = v.findViewById(R.id.btn_delete);
        }
    }
}
