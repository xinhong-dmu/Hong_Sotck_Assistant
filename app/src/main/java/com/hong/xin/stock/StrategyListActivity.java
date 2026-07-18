package com.hong.xin.stock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.StrategyManager;
import com.hong.xin.stock.data.model.Strategy;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StrategyListActivity extends AppCompatActivity {

    private RecyclerView rvStrategies;
    private StrategyListAdapter adapter;
    private StrategyManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strategy_list);

        setTitle("策略管理");

        manager = StrategyManager.getInstance(this);

        rvStrategies = findViewById(R.id.rv_strategies);
        rvStrategies.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StrategyListAdapter();
        rvStrategies.setAdapter(adapter);

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        adapter.setStrategies(manager.getAllStrategies());
    }

    private class StrategyListAdapter extends RecyclerView.Adapter<StrategyListAdapter.ViewHolder> {

        private List<Strategy> strategies;
        private Markwon markwon;

        void setStrategies(List<Strategy> list) {
            strategies = list;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_strategy, parent, false);
            if (markwon == null) {
                markwon = Markwon.builder(parent.getContext())
                        .usePlugin(TablePlugin.create(parent.getContext()))
                        .usePlugin(StrikethroughPlugin.create())
                        .build();
            }
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Strategy s = strategies.get(position);

            holder.tvName.setText(s.getName());

            holder.tvSignal.setVisibility(View.GONE);

            String stockInfo = s.getStockName() + "(" + s.getStockCode() + ")";
            holder.tvStock.setText(stockInfo);

            StringBuilder details = new StringBuilder();
            if (s.getTargetPrice() > 0) {
                details.append("目标价: ").append(String.format(Locale.getDefault(), "%.3f", s.getTargetPrice())).append("  ");
            }
            if (s.getStopLossPrice() > 0) {
                details.append("止损价: ").append(String.format(Locale.getDefault(), "%.3f", s.getStopLossPrice())).append("  ");
            }
            if (s.getConditionPriceAbove() > 0) {
                details.append("突破>").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceAbove())).append("  ");
            }
            if (s.getConditionPriceBelow() > 0) {
                details.append("跌破<").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceBelow())).append("  ");
            }
            if (s.getConditionMaAbove() > 0) {
                details.append("价格>MA").append(s.getConditionMaAbove()).append("  ");
            }
            if (!TextUtils.isEmpty(s.getConditionText())) {
                if (details.length() > 0) details.append("\n\n");
                details.append(s.getConditionText());
            }

            if (!TextUtils.isEmpty(s.getConditionText())) {
                holder.tvDetails.setMovementMethod(LinkMovementMethod.getInstance());
                markwon.setMarkdown(holder.tvDetails, details.toString());
            } else {
                holder.tvDetails.setMovementMethod(null);
                holder.tvDetails.setText(details.toString());
            }

            String status = s.isActive() ? "● 监控中" : "○ 已暂停";
            holder.tvStatus.setText(status);
            holder.tvStatus.setTextColor(s.isActive() ? 0xFF4CAF50 : 0xFF999999);

            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(s.getCreatedAt())));

            holder.itemView.setOnClickListener(v -> showStrategyDetailDialog(s));
            holder.itemView.setOnLongClickListener(v -> {
                showDeleteDialog(s);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return strategies != null ? strategies.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSignal, tvStock, tvDetails, tvStatus, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_strategy_name);
                tvSignal = itemView.findViewById(R.id.tv_strategy_signal);
                tvStock = itemView.findViewById(R.id.tv_strategy_stock);
                tvDetails = itemView.findViewById(R.id.tv_strategy_details);
                tvStatus = itemView.findViewById(R.id.tv_strategy_status);
                tvTime = itemView.findViewById(R.id.tv_strategy_time);
            }
        }
    }

    private void showStrategyDetailDialog(Strategy s) {
        StringBuilder msg = new StringBuilder();
        msg.append("股票: ").append(s.getStockName()).append("(").append(s.getStockCode()).append(")\n");

        if (!TextUtils.isEmpty(s.getConditionText())) {
            msg.append("条件: ").append(s.getConditionText()).append("\n");
        }
        if (s.getTargetPrice() > 0) {
            msg.append("目标价: ").append(String.format(Locale.getDefault(), "%.3f", s.getTargetPrice())).append("\n");
        }
        if (s.getStopLossPrice() > 0) {
            msg.append("止损价: ").append(String.format(Locale.getDefault(), "%.3f", s.getStopLossPrice())).append("\n");
        }
        if (s.getConditionPriceAbove() > 0) {
            msg.append("价格突破: >").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceAbove())).append("\n");
        }
        if (s.getConditionPriceBelow() > 0) {
            msg.append("价格跌破: <").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceBelow())).append("\n");
        }
        if (s.getConditionMaAbove() > 0) {
            msg.append("价格>MA").append(s.getConditionMaAbove()).append("\n");
        }
        msg.append("状态: ").append(s.isActive() ? "监控中" : "已暂停").append("\n");

        new AlertDialog.Builder(this)
                .setTitle(s.getName())
                .setMessage(msg.toString())
                .setPositiveButton(s.isActive() ? "暂停监控" : "恢复监控", (dialog, which) -> {
                    manager.toggleActive(s.getId(), !s.isActive());
                    refreshList();
                })
                .setNeutralButton("删除", (dialog, which) -> showDeleteDialog(s))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showDeleteDialog(Strategy s) {
        new AlertDialog.Builder(this)
                .setTitle("删除策略")
                .setMessage("确定要删除策略【" + s.getName() + "】吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    manager.deleteStrategy(s.getId());
                    refreshList();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
