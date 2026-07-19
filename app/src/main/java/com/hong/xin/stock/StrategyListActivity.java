package com.hong.xin.stock;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

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

            String stockInfo = s.getStockName() + "(" + s.getStockCode() + ")";
            holder.tvStock.setText(stockInfo);

            StringBuilder details = new StringBuilder();
            if (s.getTargetPrice() > 0) {
                details.append("止盈: ").append(String.format(Locale.getDefault(), "%.3f", s.getTargetPrice())).append("  ");
            }
            if (s.getStopLossPrice() > 0) {
                details.append("止损: ").append(String.format(Locale.getDefault(), "%.3f", s.getStopLossPrice())).append("  ");
            }
            if (s.getConditionPriceAbove() > 0) {
                details.append("突破>").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceAbove())).append("  ");
            }
            if (s.getConditionPriceBelow() > 0) {
                details.append("跌破<").append(String.format(Locale.getDefault(), "%.3f", s.getConditionPriceBelow())).append("  ");
            }
            if (s.getConditionMaAbove() > 0) {
                details.append(">MA").append(s.getConditionMaAbove()).append("  ");
            }
            if (!TextUtils.isEmpty(s.getConditionText())) {
                if (details.length() > 0) details.append("\n");
                details.append(s.getConditionText());
            }

            holder.tvDetails.setText(details.toString());

            String status = s.isActive() ? "● 监控中" : "○ 已暂停";
            holder.tvStatus.setText(status);
            holder.tvStatus.setTextColor(s.isActive() ?
                    getResources().getColor(R.color.green, null) :
                    getResources().getColor(R.color.text_secondary, null));

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
            TextView tvName, tvStock, tvDetails, tvStatus, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_strategy_name);
                tvStock = itemView.findViewById(R.id.tv_strategy_stock);
                tvDetails = itemView.findViewById(R.id.tv_strategy_details);
                tvStatus = itemView.findViewById(R.id.tv_strategy_status);
                tvTime = itemView.findViewById(R.id.tv_strategy_time);
            }
        }
    }

    private void showStrategyDetailDialog(Strategy s) {
        Markwon markwon = Markwon.builder(this)
                .usePlugin(TablePlugin.create(this))
                .usePlugin(StrikethroughPlugin.create())
                .build();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scrollView.setScrollbarFadingEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        scrollView.addView(layout);

        TextView tvStock = new TextView(this);
        tvStock.setText("股票: " + s.getStockName() + "(" + s.getStockCode() + ")");
        tvStock.setTextSize(14);
        tvStock.setTextColor(getResources().getColor(R.color.text_primary, null));
        layout.addView(tvStock);

        if (s.getTargetPrice() > 0) {
            TextView tv = new TextView(this);
            tv.setText("止盈价: " + String.format(Locale.getDefault(), "%.3f", s.getTargetPrice()));
            tv.setTextSize(14);
            tv.setTextColor(getResources().getColor(R.color.green, null));
            tv.setPadding(0, 6, 0, 0);
            layout.addView(tv);
        }

        if (s.getStopLossPrice() > 0) {
            TextView tv = new TextView(this);
            tv.setText("止损价: " + String.format(Locale.getDefault(), "%.3f", s.getStopLossPrice()));
            tv.setTextSize(14);
            tv.setTextColor(getResources().getColor(R.color.red, null));
            tv.setPadding(0, 2, 0, 0);
            layout.addView(tv);
        }

        if (!TextUtils.isEmpty(s.getConditionText())) {
            TextView tv = new TextView(this);
            tv.setText("条件: " + s.getConditionText());
            tv.setTextSize(13);
            tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tv.setPadding(0, 8, 0, 0);
            layout.addView(tv);
        }

        TextView tvStatus = new TextView(this);
        tvStatus.setText("状态: " + (s.isActive() ? "监控中" : "已暂停"));
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(s.isActive() ?
                getResources().getColor(R.color.green, null) :
                getResources().getColor(R.color.text_secondary, null));
        tvStatus.setPadding(0, 8, 0, 0);
        layout.addView(tvStatus);

        if (!TextUtils.isEmpty(s.getAiMessage())) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(getResources().getColor(R.color.divider_light, null));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
            params.setMargins(0, 12, 0, 8);
            divider.setLayoutParams(params);
            layout.addView(divider);

            TextView tvAi = new TextView(this);
            tvAi.setMovementMethod(LinkMovementMethod.getInstance());
            markwon.setMarkdown(tvAi, s.getAiMessage());
            tvAi.setTextSize(13);
            tvAi.setLinkTextColor(getResources().getColor(R.color.primary, null));
            layout.addView(tvAi);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        TextView tvTime = new TextView(this);
        tvTime.setText("创建时间: " + sdf.format(new Date(s.getCreatedAt())));
        tvTime.setTextSize(12);
        tvTime.setTextColor(getResources().getColor(R.color.text_hint, null));
        tvTime.setPadding(0, 12, 0, 0);
        layout.addView(tvTime);

        new AlertDialog.Builder(this)
                .setTitle(s.getName())
                .setView(scrollView)
                .setPositiveButton("编辑", (dialog, which) -> showEditDialog(s))
                .setNeutralButton(s.isActive() ? "暂停" : "启用", (dialog, which) -> {
                    manager.toggleActive(s.getId(), !s.isActive());
                    refreshList();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showEditDialog(Strategy s) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        scrollView.addView(layout);

        TextView labelName = new TextView(this);
        labelName.setText("策略名称");
        labelName.setTextSize(13);
        labelName.setTextColor(getResources().getColor(R.color.text_primary, null));
        layout.addView(labelName);

        EditText etName = new EditText(this);
        etName.setText(s.getName());
        etName.setTextSize(13);
        etName.setPadding(16, 12, 16, 12);
        etName.setBackgroundResource(R.drawable.bg_input);
        layout.addView(etName);

        TextView labelTarget = new TextView(this);
        labelTarget.setText("止盈价格（价格高于此值触发）");
        labelTarget.setTextSize(13);
        labelTarget.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelTarget.setPadding(0, 12, 0, 0);
        layout.addView(labelTarget);

        EditText etTarget = new EditText(this);
        if (s.getTargetPrice() > 0) {
            etTarget.setText(String.format(Locale.getDefault(), "%.3f", s.getTargetPrice()));
        }
        etTarget.setHint("例如: 10.50");
        etTarget.setTextSize(13);
        etTarget.setPadding(16, 12, 16, 12);
        etTarget.setBackgroundResource(R.drawable.bg_input);
        etTarget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etTarget);

        TextView labelStopLoss = new TextView(this);
        labelStopLoss.setText("止损价格（价格低于此值触发）");
        labelStopLoss.setTextSize(13);
        labelStopLoss.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelStopLoss.setPadding(0, 12, 0, 0);
        layout.addView(labelStopLoss);

        EditText etStopLoss = new EditText(this);
        if (s.getStopLossPrice() > 0) {
            etStopLoss.setText(String.format(Locale.getDefault(), "%.3f", s.getStopLossPrice()));
        }
        etStopLoss.setHint("例如: 8.00");
        etStopLoss.setTextSize(13);
        etStopLoss.setPadding(16, 12, 16, 12);
        etStopLoss.setBackgroundResource(R.drawable.bg_input);
        etStopLoss.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etStopLoss);

        new AlertDialog.Builder(this)
                .setTitle("编辑策略")
                .setView(scrollView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "请输入策略名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    s.setName(name);

                    try {
                        String tp = etTarget.getText().toString().trim();
                        if (!tp.isEmpty()) s.setTargetPrice(Double.parseDouble(tp));
                        else s.setTargetPrice(0);
                    } catch (NumberFormatException ignored) {}

                    try {
                        String sl = etStopLoss.getText().toString().trim();
                        if (!sl.isEmpty()) s.setStopLossPrice(Double.parseDouble(sl));
                        else s.setStopLossPrice(0);
                    } catch (NumberFormatException ignored) {}

                    manager.saveStrategy(s);
                    refreshList();
                    Toast.makeText(this, "策略已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
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
