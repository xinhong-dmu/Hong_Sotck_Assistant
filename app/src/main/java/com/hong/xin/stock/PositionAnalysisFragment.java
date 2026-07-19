package com.hong.xin.stock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.ChatHistoryManager;
import com.hong.xin.stock.data.PromptTemplateManager;
import com.hong.xin.stock.data.PurchaseRecordManager;
import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.StrategyManager;
import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.model.ChatMessage;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;
import com.hong.xin.stock.data.model.PurchaseRecord;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.model.Strategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PositionAnalysisFragment extends Fragment {

    private static final String PREF_NAME = "deepseek_config";
    private static final String CHAT_KEY = "position_analysis";

    private LinearLayout positionContainer;
    private View contextBar;
    private RecyclerView rvChat;
    private EditText etInput;
    private TextView btnSend;
    private ChatAdapter chatAdapter;

    private ChatHistoryManager chatHistoryManager;
    private PromptTemplateManager templateManager;
    private PurchaseRecordManager purchaseRecordManager;
    private List<PositionStock> positionStocks = new ArrayList<>();
    private View rootView;

    private List<KlineData> marketKlines;
    private boolean dataLoaded = false;
    private boolean loading = false;
    private String customSystemPrompt;
    private String commonSystemPrompt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_position_analysis, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatHistoryManager = ChatHistoryManager.getInstance(requireContext());
        templateManager = PromptTemplateManager.getInstance(requireContext());
        purchaseRecordManager = PurchaseRecordManager.getInstance(requireContext());

        initViews();
        checkToken();
    }

    private void initViews() {
        positionContainer = rootView.findViewById(R.id.position_container);
        contextBar = rootView.findViewById(R.id.context_bar);
        rvChat = rootView.findViewById(R.id.rv_chat);
        etInput = rootView.findViewById(R.id.et_input);
        btnSend = rootView.findViewById(R.id.btn_send);

        TextView btnTemplates = rootView.findViewById(R.id.btn_templates);
        TextView btnClearHistory = rootView.findViewById(R.id.btn_clear_history);

        chatAdapter = new ChatAdapter();
        chatAdapter.setOnStrategySaveListener(this::showStrategySaveDialog);
        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
        btnTemplates.setOnClickListener(v -> showTemplateDialog());
        btnClearHistory.setOnClickListener(v -> clearHistory());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void checkToken() {
        String apiKey = DeepSeekApi.getApiKey(requireContext());
        if (TextUtils.isEmpty(apiKey)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("未配置API Key")
                    .setMessage("请先在设置中配置DeepSeek API Key后再使用AI分析功能。")
                    .setPositiveButton("去设置", (d, w) -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).switchToTab(2);
                        }
                    })
                    .setNegativeButton("关闭", null)
                    .setCancelable(false)
                    .show();
            return;
        }
        etInput.setVisibility(View.VISIBLE);
        btnSend.setVisibility(View.VISIBLE);
        loadCommonPrompt();
        loadCustomPrompt();
        loadPositionStocks();
        loadContextData();
    }

    private void loadPositionStocks() {
        positionContainer.removeAllViews();

        Map<String, PositionStock> oldStockMap = new HashMap<>();
        for (PositionStock old : positionStocks) {
            oldStockMap.put(old.stockCode, old);
        }
        positionStocks.clear();

        SelectedStockManager stockMgr = SelectedStockManager.getInstance(requireContext());
        Map<String, Stock> stockMap = new HashMap<>();
        for (Stock s : stockMgr.getSelectedStocks()) {
            stockMap.put(s.getCode(), s);
        }

        for (Map.Entry<String, List<PurchaseRecord>> entry : purchaseRecordManager.getAllRecords().entrySet()) {
            List<PurchaseRecord> records = entry.getValue();
            if (records == null || records.isEmpty()) continue;

            Stock stock = stockMap.get(entry.getKey());
            if (stock == null) {
                stock = new Stock(entry.getKey(), entry.getKey(), Stock.TYPE_STOCK);
            }

            double avgCost = 0;
            for (PurchaseRecord r : records) avgCost += r.getPrice();
            avgCost /= records.size();

            PositionStock oldPs = oldStockMap.get(entry.getKey());

            PositionStock ps = new PositionStock();
            ps.stockCode = stock.getCode();
            ps.stockName = stock.getName();
            ps.stockType = stock.getType();
            ps.avgCost = avgCost;
            ps.recordCount = records.size();
            ps.purchaseRecords = records;
            if (oldPs != null) {
                ps.quote = oldPs.quote;
                ps.klines = oldPs.klines;
                ps.minuteData = oldPs.minuteData;
            }

            positionStocks.add(ps);

            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(0, 2, 0, 2);
            container.setBackgroundResource(R.drawable.bg_card);
            container.setTag(ps);
            container.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), StockDetailActivity.class);
                intent.putExtra("code", ps.stockCode);
                intent.putExtra("name", ps.stockName);
                intent.putExtra("type", ps.stockType);
                startActivity(intent);
            });

            LinearLayout row1 = new LinearLayout(requireContext());
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(stock.getName());
            tvName.setTextSize(13);
            tvName.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
            row1.addView(tvName);

            TextView tvPrice = new TextView(requireContext());
            tvPrice.setTextSize(12);
            tvPrice.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvPrice.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvPrice.setGravity(android.view.Gravity.CENTER);
            row1.addView(tvPrice);
            ps.priceView = tvPrice;

            TextView tvPnL = new TextView(requireContext());
            tvPnL.setTextSize(12);
            tvPnL.setPadding(2, 0, 2, 0);
            tvPnL.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvPnL.setGravity(android.view.Gravity.CENTER);
            row1.addView(tvPnL);
            ps.pnlView = tvPnL;

            TextView btnDel = new TextView(requireContext());
            btnDel.setText("×");
            btnDel.setTextSize(16);
            btnDel.setTextColor(getResources().getColor(R.color.red, null));
            btnDel.setPadding(12, 4, 8, 4);
            btnDel.setOnClickListener(v -> showDeleteRecordDialog(ps));
            row1.addView(btnDel);

            container.addView(row1);

            LinearLayout row2 = new LinearLayout(requireContext());
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setPadding(0, 1, 0, 0);

            StringBuilder detail = new StringBuilder();
            detail.append("均价").append(new DecimalFormat("#0.000").format(avgCost));
            detail.append(" ").append(ps.recordCount).append("笔 (");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) detail.append("; ");
                PurchaseRecord r = records.get(i);
                detail.append(new DecimalFormat("#0.000").format(r.getPrice()));
                if (r.getDate() != null && r.getDate().length() > 5) {
                    detail.append("@").append(r.getDate().substring(5));
                }
            }
            detail.append(")");

            TextView tvCost = new TextView(requireContext());
            tvCost.setText(detail.toString());
            tvCost.setTextSize(10);
            tvCost.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvCost.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row2.addView(tvCost);

            container.addView(row2);

            positionContainer.addView(container);

            updatePnlDisplay(ps);
        }

        if (positionStocks.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂未添加持仓股票\n请在股票详情页录入买入价格和日期");
            empty.setTextSize(13);
            empty.setTextColor(getResources().getColor(R.color.text_secondary, null));
            empty.setPadding(16, 16, 16, 16);
            empty.setGravity(android.view.Gravity.CENTER);
            positionContainer.addView(empty);
        }
    }

    private void showDeleteRecordDialog(PositionStock ps) {
        List<PurchaseRecord> records = purchaseRecordManager.getRecords(ps.stockCode);
        if (records.isEmpty()) {
            Toast.makeText(requireContext(), "暂无持仓记录", Toast.LENGTH_SHORT).show();
            return;
        }

        DecimalFormat df = new DecimalFormat("#0.000");
        String[] items = new String[records.size() + 1];
        for (int i = 0; i < records.size(); i++) {
            items[i] = df.format(records.get(i).getPrice()) + " @" + records.get(i).getDate();
        }
        items[records.size()] = "全部删除（" + records.size() + "笔）";

        ListView listView = new ListView(requireContext());
        listView.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items));
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setScrollbarFadingEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("删除持仓：" + ps.stockName)
                .setView(listView)
                .setNegativeButton("关闭", null)
                .show();

        listView.setOnItemClickListener((parent, view, which, id) -> {
            dialog.dismiss();
            if (which < records.size()) {
                PurchaseRecord r = records.get(which);
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("删除该笔记录：" + df.format(r.getPrice()) + " " + r.getDate() + "？")
                        .setPositiveButton("确定", (d, w) -> {
                            purchaseRecordManager.deleteRecord(ps.stockCode, r.getId());
                            loadPositionStocks();
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认全部删除")
                        .setMessage("确定删除 " + ps.stockName + " 的全部" + records.size() + "笔持仓记录？")
                        .setPositiveButton("确定", (d, w) -> {
                            purchaseRecordManager.deleteAllRecords(ps.stockCode);
                            loadPositionStocks();
                            Toast.makeText(requireContext(), ps.stockName + " 持仓已全部删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    private void updatePnlDisplay(PositionStock ps) {
        if (ps.quote != null && ps.quote.getPrice() > 0) {
            DecimalFormat df = new DecimalFormat("#0.000");
            if (ps.priceView != null) {
                ps.priceView.setText(df.format(ps.quote.getPrice()));
            }
            if (ps.pnlView != null && ps.avgCost > 0) {
                double pnl = ps.quote.getPrice() - ps.avgCost;
                double pnlPct = (pnl / ps.avgCost) * 100;
                ps.pnlView.setText(String.format(Locale.getDefault(), "%+.2f%%", pnlPct));
                ps.pnlView.setTextColor(pnlPct >= 0 ?
                        requireContext().getResources().getColor(R.color.red) :
                        requireContext().getResources().getColor(R.color.green));
            }
        } else {
            if (ps.priceView != null) ps.priceView.setText("--");
            if (ps.pnlView != null) ps.pnlView.setText("--");
        }
    }

    private void showPromptEditDialog() {
        String currentPrompt = getEffectiveInstruction();

        EditText input = new EditText(requireContext());
        input.setText(currentPrompt);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(10);
        input.setGravity(android.view.Gravity.TOP);
        input.setPadding(24, 24, 24, 24);
        input.setTextSize(13);
        input.setHorizontallyScrolling(false);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑持仓分析策略指令")
                .setMessage("仅编辑分析策略指令，所有持仓行情数据会自动附加。")
                .setView(scrollView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        customSystemPrompt = null;
                        saveCustomPrompt("");
                        Toast.makeText(requireContext(), "已恢复默认提示词", Toast.LENGTH_SHORT).show();
                    } else {
                        customSystemPrompt = text;
                        saveCustomPrompt(text);
                        Toast.makeText(requireContext(), "提示词已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("恢复默认", (dialog, which) -> {
                    customSystemPrompt = null;
                    saveCustomPrompt("");
                    Toast.makeText(requireContext(), "已恢复默认提示词", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getDefaultInstruction() {
        return "你是一个专业的A股持仓组合分析师。请根据以下多只持仓股票的实时行情和盘后数据，从组合管理角度进行全面分析。\n" +
               "分析内容应包括：\n" +
               "1. 每只持仓股的技术面分析（K线形态、均线、支撑阻力等）\n" +
               "2. 基本面概况（PE/PB/市值/换手率等关键指标）\n" +
               "3. 与大盘走势的联动分析\n" +
               "4. 分时量体现的资金动向\n" +
               "5. 整体仓位配置建议\n" +
               "6. 风险提示与市场情绪判断\n\n" +
               "请优先输出总结论，然后以表格形式列出每只股票的具体策略：\n" +
               "| 股票 | 当前仓位评估 | 目标价 | 止损价 | 操作建议 | 建议仓位比例 | 关键理由 |\n" +
               "|------|-------------|--------|--------|---------|-------------|---------|\n" +
               "表格后再展开详细分析。回答请使用中文，要求：精简扼要，重点突出。";
    }

    private String getEffectiveInstruction() {
        if (!TextUtils.isEmpty(customSystemPrompt)) {
            return customSystemPrompt;
        }
        if (!TextUtils.isEmpty(commonSystemPrompt)) {
            return commonSystemPrompt;
        }
        return getDefaultInstruction();
    }

    private String buildDataSections() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 持仓组合概况 ===\n");
        sb.append("持仓股票数量：").append(positionStocks.size()).append("只\n\n");

        DecimalFormat df3 = new DecimalFormat("#0.000");
        double totalCost = 0;
        double totalValue = 0;
        int validCount = 0;

        for (int i = 0; i < positionStocks.size(); i++) {
            PositionStock ps = positionStocks.get(i);
            sb.append(buildSingleStockSection(ps, i + 1));

            if (ps.avgCost > 0) totalCost += ps.avgCost;
            if (ps.quote != null && ps.quote.getPrice() > 0) {
                totalValue += ps.quote.getPrice();
                validCount++;
            }
        }
        if (validCount > 0 && totalCost > 0) {
            double totalPnl = totalValue - totalCost;
            double totalPnlPct = (totalPnl / totalCost) * 100;
            sb.append("=== 组合总盈亏 ===\n");
            sb.append("总成本：").append(df3.format(totalCost));
            sb.append("  总现值：").append(df3.format(totalValue));
            sb.append("  浮动盈亏：").append(String.format(Locale.getDefault(), "%.3f", totalPnl));
            sb.append("（").append(String.format(Locale.getDefault(), "%.3f%%", totalPnlPct)).append("）\n\n");
        }

        sb.append(buildMarketKlineSection());
        return sb.toString();
    }

    private String buildSingleStockSection(PositionStock ps, int index) {
        DecimalFormat df = new DecimalFormat("#0.00");
        DecimalFormat df3 = new DecimalFormat("#0.000");

        StringBuilder sb = new StringBuilder();
        sb.append("=== 持仓").append(index).append("：").append(ps.stockName).append("(").append(ps.stockCode).append(") ===\n");
        sb.append("买入均价：").append(df3.format(ps.avgCost));
        sb.append("  买入笔数：").append(ps.recordCount).append("\n");
        sb.append("买入记录：");
        boolean first = true;
        for (PurchaseRecord r : ps.purchaseRecords) {
            if (!first) sb.append("; ");
            sb.append(df3.format(r.getPrice())).append("@").append(r.getDate());
            first = false;
        }
        sb.append("\n");

        if (ps.quote != null && ps.quote.getPrice() > 0) {
            double cur = ps.quote.getPrice();
            double pnl = cur - ps.avgCost;
            double pnlPct = (pnl / ps.avgCost) * 100;
            sb.append("最新价：").append(df3.format(cur));
            sb.append("（").append(String.format(Locale.getDefault(), "%+.3f", pnl));
            sb.append("，").append(String.format(Locale.getDefault(), "%+.3f%%", pnlPct)).append("）\n");

            sb.append("开盘：").append(df3.format(ps.quote.getOpen()));
            sb.append("  最高：").append(df3.format(ps.quote.getHigh()));
            sb.append("  最低：").append(df3.format(ps.quote.getLow()));
            sb.append("  昨收：").append(df3.format(ps.quote.getPreClose())).append("\n");
            sb.append("成交量：").append(formatVolume(ps.quote.getVolume()));
            sb.append("  成交额：").append(formatAmount(ps.quote.getAmount()));
            sb.append("  换手率：").append(ps.quote.getTurnoverRate() > 0 ? df.format(ps.quote.getTurnoverRate()) + "%" : "--").append("\n");
            sb.append("市盈率(静态)：").append(ps.quote.getPe() > 0 ? df.format(ps.quote.getPe()) : "--");
            sb.append("  市盈率(TTM)：").append(ps.quote.getPeTTM() > 0 ? df.format(ps.quote.getPeTTM()) : "--");
            sb.append("  市净率：").append(ps.quote.getPb() > 0 ? df.format(ps.quote.getPb()) : "--").append("\n");
            sb.append("总市值：").append(formatMarketCap(ps.quote.getTotalMarketCap()));
            sb.append("  流通市值：").append(formatMarketCap(ps.quote.getCirculatingMarketCap())).append("\n");
            sb.append("量比：").append(ps.quote.getVolumeRatio() > 0 ? df.format(ps.quote.getVolumeRatio()) : "--");
            sb.append("  涨停价：").append(df3.format(ps.quote.getLimitUp()));
            sb.append("  跌停价：").append(df3.format(ps.quote.getLimitDown())).append("\n");
            if (ps.quote.getMa5() > 0) {
                sb.append("均线：MA5=").append(df3.format(ps.quote.getMa5()));
                sb.append(" MA10=").append(df3.format(ps.quote.getMa10()));
                sb.append(" MA20=").append(df3.format(ps.quote.getMa20()));
                sb.append(" MA30=").append(df3.format(ps.quote.getMa30()));
                sb.append(" MA60=").append(df3.format(ps.quote.getMa60())).append("\n");
            }
            if (ps.stockType != null && ps.stockType.equals("etf") && ps.quote.getIopv() > 0) {
                sb.append("IOPV：").append(df3.format(ps.quote.getIopv()));
                sb.append("  溢价率：").append(df.format(ps.quote.getPremiumRate())).append("%\n");
            }
        } else {
            sb.append("(行情数据加载中...)\n");
        }

        if (ps.klines != null && !ps.klines.isEmpty()) {
            int show = Math.min(ps.klines.size(), 120);
            sb.append("--- ").append(show).append("日K线 ---\n");
            sb.append("日期        开盘     最高     最低     收盘     涨跌幅    成交量(手)\n");
            int start = ps.klines.size() - show;
            double highest = ps.klines.get(start).getHigh();
            double lowest = ps.klines.get(start).getLow();
            for (int i = start; i < ps.klines.size(); i++) {
                KlineData k = ps.klines.get(i);
                sb.append(k.getDate()).append("  ");
                sb.append(df.format(k.getOpen())).append("  ");
                sb.append(df.format(k.getHigh())).append("  ");
                sb.append(df.format(k.getLow())).append("  ");
                sb.append(df3.format(k.getClose())).append("  ");
                sb.append(String.format(Locale.getDefault(), "%+.2f%%", k.getPctChg())).append("  ");
                sb.append(formatVolumeInt(k.getVolume())).append("\n");
                if (k.getHigh() > highest) highest = k.getHigh();
                if (k.getLow() < lowest) lowest = k.getLow();
            }
            sb.append(show).append("日区间：最高 ").append(df3.format(highest));
            sb.append("  最低 ").append(df3.format(lowest));
            sb.append("  幅度 ").append(String.format(Locale.getDefault(), "%.2f%%", (highest - lowest) / lowest * 100)).append("\n");
        }

        if (ps.minuteData != null && !ps.minuteData.isEmpty()) {
            sb.append("--- 20日分时量概况 ---\n");
            sb.append("数据条数：").append(ps.minuteData.size()).append("\n");
            double high = 0, low = Double.MAX_VALUE;
            for (MinuteLineData d : ps.minuteData) {
                if (d.getPrice() > high) high = d.getPrice();
                if (d.getPrice() > 0 && d.getPrice() < low) low = d.getPrice();
            }
            sb.append("区间最高：").append(df3.format(high));
            sb.append("  区间最低：").append(df3.format(low)).append("\n");

            int detailDays = 1;
            String today = "";
            if (!ps.minuteData.isEmpty()) {
                String t = ps.minuteData.get(ps.minuteData.size() - 1).getTime();
                if (t.length() >= 10) today = t.substring(0, 10);
            }
            sb.append("最近1日分时明细（时间,价格,均价,成交量）：\n");
            int shown = 0;
            for (int i = ps.minuteData.size() - 1; i >= 0; i--) {
                MinuteLineData d = ps.minuteData.get(i);
                String date = d.getTime().length() >= 10 ? d.getTime().substring(0, 10) : "";
                if (!date.equals(today)) {
                    today = date;
                    shown++;
                    if (shown > detailDays) break;
                }
                sb.append(d.getTime()).append(",");
                sb.append(df3.format(d.getPrice())).append(",");
                sb.append(df3.format(d.getAvgPrice())).append(",");
                sb.append((int) d.getVolume()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildMarketKlineSection() {
        if (marketKlines == null || marketKlines.isEmpty()) return "";

        DecimalFormat df = new DecimalFormat("#0.00");
        DecimalFormat df3 = new DecimalFormat("#0.000");

        StringBuilder sb = new StringBuilder();
        int show = Math.min(marketKlines.size(), 120);
        sb.append("=== 上证指数K线（最近").append(show).append("个交易日） ===\n");
        sb.append("日期        开盘     最高     最低     收盘     涨跌幅\n");

        int start = marketKlines.size() - show;
        for (int i = start; i < marketKlines.size(); i++) {
            KlineData k = marketKlines.get(i);
            sb.append(k.getDate()).append("  ");
            sb.append(df.format(k.getOpen())).append("  ");
            sb.append(df.format(k.getHigh())).append("  ");
            sb.append(df.format(k.getLow())).append("  ");
            sb.append(df3.format(k.getClose())).append("  ");
            sb.append(String.format(Locale.getDefault(), "%+.2f%%", k.getPctChg())).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildSystemPrompt() {
        return getEffectiveInstruction() + "\n\n" + buildDataSections();
    }

    private void saveCustomPrompt(String text) {
        requireContext().getSharedPreferences(PREF_NAME, 0)
                .edit().putString("position_analysis_prompt", text).apply();
    }

    private void loadCustomPrompt() {
        customSystemPrompt = requireContext().getSharedPreferences(PREF_NAME, 0)
                .getString("position_analysis_prompt", null);
    }

    private void loadCommonPrompt() {
        commonSystemPrompt = requireContext().getSharedPreferences(PREF_NAME, 0)
                .getString("common_prompt", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataLoaded) {
            loadCommonPrompt();
            loadCustomPrompt();
            int oldCount = positionStocks.size();
            List<String> oldCodes = new ArrayList<>();
            for (PositionStock ps : positionStocks) oldCodes.add(ps.stockCode);

            loadPositionStocks();

            boolean stocksChanged = positionStocks.size() != oldCount;
            if (!stocksChanged) {
                for (PositionStock ps : positionStocks) {
                    if (!oldCodes.contains(ps.stockCode)) {
                        stocksChanged = true;
                        break;
                    }
                }
            }
            if (stocksChanged) {
                loadContextData();
            }
        }
    }

    private void showModelDialog() {
        String[] models = {"deepseek-v4-pro", "deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner", "\u2728 自定义..."};
        int currentIdx = -1;
        String currentModel = DeepSeekApi.getModel(requireContext());
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(currentModel)) {
                currentIdx = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("选择模型")
                .setSingleChoiceItems(models, Math.max(currentIdx, 0), null)
                .setPositiveButton("确定", (dialog, which) -> {
                    int pos = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (pos == models.length - 1) {
                        showCustomModelDialog();
                    } else if (pos >= 0) {
                        DeepSeekApi.setModel(requireContext(), models[pos]);
                        Toast.makeText(requireContext(), "已切换为 " + models[pos], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCustomModelDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入模型ID");
        input.setPadding(24, 24, 24, 24);
        input.setText(DeepSeekApi.getModel(requireContext()));

        new AlertDialog.Builder(requireContext())
                .setTitle("自定义模型")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String model = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(model)) {
                        DeepSeekApi.setModel(requireContext(), model);
                        Toast.makeText(requireContext(), "已切换为 " + model, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadContextData() {
        if (loading) return;
        loading = true;

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                chatAdapter.addMessage(new ChatMessage("assistant", "正在加载" + positionStocks.size() + "只持仓股票数据..."));
                rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            });
        }

        int requestCount = 1;
        for (PositionStock ps : positionStocks) {
            requestCount += 4;
        }
        final int[] pending = {requestCount};

        for (PositionStock ps : positionStocks) {
            EastMoneyApi.fetchRealtime(ps.stockCode, q -> {
                ps.quote = q;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updatePnlDisplay(ps));
                }
                if (--pending[0] <= 0) onDataReady();
            });

            EastMoneyApi.fetchExtra(ps.stockCode, q -> {
                if (ps.quote == null) {
                    ps.quote = q;
                } else if (q.getPe() > 0 || q.getIopv() > 0) {
                    ps.quote = new RealtimeQuote.Builder(ps.quote)
                            .pe(q.getPe())
                            .pb(q.getPb())
                            .turnoverRate(q.getTurnoverRate())
                            .volumeRatio(q.getVolumeRatio())
                            .totalMarketCap(q.getTotalMarketCap())
                            .circulatingMarketCap(q.getCirculatingMarketCap())
                            .limitUp(q.getLimitUp())
                            .limitDown(q.getLimitDown())
                            .eps(q.getEps())
                            .dividendYield(q.getDividendYield())
                            .ma5(q.getMa5())
                            .ma10(q.getMa10())
                            .ma20(q.getMa20())
                            .ma30(q.getMa30())
                            .ma60(q.getMa60())
                            .iopv(q.getIopv())
                            .premiumRate(q.getPremiumRate())
                            .build();
                }
                if (--pending[0] <= 0) onDataReady();
            });

            EastMoneyApi.fetchKline(ps.stockCode, 120, klines -> {
                ps.klines = klines != null ? klines : new ArrayList<>();
                if (--pending[0] <= 0) onDataReady();
            });

            EastMoneyApi.fetchMinuteLineDays(ps.stockCode, 20, data -> {
                ps.minuteData = data != null ? data : new ArrayList<>();
                if (--pending[0] <= 0) onDataReady();
            });
        }

        EastMoneyApi.fetchKline("000001", 120, klines -> {
            marketKlines = klines != null ? klines : new ArrayList<>();
            if (--pending[0] <= 0) onDataReady();
        });
    }

    private void onDataReady() {
        dataLoaded = true;

        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            chatAdapter.getMessages().clear();
            chatAdapter.notifyDataSetChanged();

            List<ChatMessage> saved = chatHistoryManager.getMessages(CHAT_KEY);
            if (!saved.isEmpty()) {
                chatAdapter.addMessages(saved);
            } else {
                showWelcomeMessage();
            }
        });
    }

    private void showWelcomeMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("你好！我是持仓组合分析助手。\n\n");
        sb.append("已加载").append(positionStocks.size()).append("只持仓股票：\n");

        DecimalFormat df3 = new DecimalFormat("#0.000");
        for (int i = 0; i < positionStocks.size(); i++) {
            PositionStock ps = positionStocks.get(i);
            sb.append((i + 1)).append(".").append(ps.stockName).append("(").append(ps.stockCode).append(")");
            sb.append("  均价：").append(df3.format(ps.avgCost));
            sb.append("  共").append(ps.recordCount).append("笔");

            if (ps.quote != null && ps.quote.getPrice() > 0) {
                double pnl = ps.quote.getPrice() - ps.avgCost;
                double pnlPct = (pnl / ps.avgCost) * 100;
                sb.append("  浮动盈亏：").append(String.format(Locale.getDefault(), "%+.3f (%+.3f%%)", pnl, pnlPct));
            }
            sb.append("\n");

            if (ps.klines != null) {
                sb.append("    已加载").append(ps.klines.size()).append("日K线");
            }
            if (ps.minuteData != null) {
                sb.append("、").append(ps.minuteData.size()).append("条分时量");
            }
            sb.append("\n");
        }

        if (marketKlines != null && !marketKlines.isEmpty()) {
            sb.append("\n已加载上证指数").append(marketKlines.size()).append("日K线。\n");
        }
        sb.append("\n可以直接提问进行持仓组合分析，我会综合所有持仓股票数据给出分析预测。\n");
        sb.append("建议提问：请对当前持仓组合进行全面分析并给出策略表。");

        ChatMessage welcomeMsg = new ChatMessage("assistant", sb.toString());
        chatAdapter.addMessage(welcomeMsg);
        chatHistoryManager.addMessage(CHAT_KEY, welcomeMsg);
    }

    private void clearHistory() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空对话")
                .setMessage("确定要清空所有对话记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    chatHistoryManager.clearHistory(CHAT_KEY);
                    chatAdapter.getMessages().clear();
                    chatAdapter.notifyDataSetChanged();
                    showWelcomeMessage();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String apiKey = DeepSeekApi.getApiKey(requireContext());
        if (TextUtils.isEmpty(apiKey)) {
            checkToken();
            return;
        }

        etInput.setText("");

        ChatMessage userMsg = new ChatMessage("user", text);
        chatAdapter.addMessage(userMsg);
        chatHistoryManager.addMessage(CHAT_KEY, userMsg);

        ChatMessage loadingMsg = new ChatMessage("assistant", "⏳ 正在分析持仓数据...");
        chatAdapter.addMessage(loadingMsg);
        rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        btnSend.setEnabled(false);
        btnSend.setText("...");

        List<ChatMessage> apiMessages = new ArrayList<>();
        apiMessages.add(new ChatMessage("system", buildSystemPrompt()));
        List<ChatMessage> adapterMsgs = chatAdapter.getMessages();
        for (int i = 0; i < adapterMsgs.size() - 1; i++) {
            apiMessages.add(adapterMsgs.get(i));
        }

        DeepSeekApi.chat(requireContext(), apiMessages, new DeepSeekApi.ChatCallback() {
            @Override
            public void onResult(String content) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    List<ChatMessage> msgs = chatAdapter.getMessages();
                    msgs.remove(msgs.size() - 1);
                    chatAdapter.notifyItemRemoved(msgs.size());

                    ChatMessage assistantMsg = new ChatMessage("assistant", content);
                    chatAdapter.addMessage(assistantMsg);
                    chatHistoryManager.addMessage(CHAT_KEY, assistantMsg);
                    rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    btnSend.setEnabled(true);
                    btnSend.setText("发送");
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    List<ChatMessage> msgs = chatAdapter.getMessages();
                    msgs.remove(msgs.size() - 1);
                    chatAdapter.notifyItemRemoved(msgs.size());
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                    btnSend.setText("发送");
                });
            }
        });
    }

    private void showTemplateDialog() {
        Map<String, String> templates = templateManager.getTemplates();
        List<String> names = templateManager.getTemplateNames();

        if (names.isEmpty()) {
            showTemplateEditDialog("", "");
            return;
        }

        String[] items = new String[names.size() + 1];
        for (int i = 0; i < names.size(); i++) {
            items[i] = names.get(i);
        }
        items[names.size()] = "+ 新建模板";

        ListView listView = new ListView(requireContext());
        listView.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items));
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setScrollbarFadingEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("选择Prompt模板")
                .setView(listView)
                .setNegativeButton("关闭", null)
                .show();

        listView.setOnItemClickListener((parent, view, which, id) -> {
            dialog.dismiss();
            if (which < names.size()) {
                String content = templateManager.getTemplate(names.get(which));
                appendContext(content);
            } else {
                showTemplateEditDialog("", "");
            }
        });
    }

    private void showTemplateEditDialog(String name, String content) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_template_edit, null);
        EditText etName = view.findViewById(R.id.et_template_name);
        EditText etContent = view.findViewById(R.id.et_template_content);

        if (!TextUtils.isEmpty(name)) etName.setText(name);
        if (!TextUtils.isEmpty(content)) etContent.setText(content);

        new AlertDialog.Builder(requireContext())
                .setTitle(TextUtils.isEmpty(name) ? "新建模板" : "编辑模板")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newContent = etContent.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(requireContext(), "请输入模板名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(newContent)) {
                        Toast.makeText(requireContext(), "请输入模板内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!TextUtils.isEmpty(name) && !name.equals(newName)) {
                        templateManager.deleteTemplate(name);
                    }
                    templateManager.saveTemplate(newName, newContent);
                    Toast.makeText(requireContext(), "模板已保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void appendContext(String text) {
        String current = etInput.getText().toString();
        if (!TextUtils.isEmpty(current)) {
            current += "\n\n";
        }
        current += text;
        etInput.setText(current);
        etInput.setSelection(current.length());
        Toast.makeText(requireContext(), "已添加到输入框，可编辑后发送", Toast.LENGTH_SHORT).show();
    }

    private void showStrategySaveDialog(ChatMessage message) {
        String aiContent = message.getContent();
        String defaultCondition = extractConditionText(aiContent, "");
        String defaultTarget = extractPrice(aiContent, "目标");
        String defaultStopLoss = extractPrice(aiContent, "止损");
        String defaultPriceAbove = extractPrice(aiContent, "突破");
        String defaultPriceBelow = extractPrice(aiContent, "跌破");

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        scrollView.addView(layout);

        TextView labelStock = new TextView(requireContext());
        labelStock.setText("关联股票");
        labelStock.setTextSize(13);
        labelStock.setTextColor(getResources().getColor(R.color.text_primary, null));
        layout.addView(labelStock);

        Spinner spinnerStock = new Spinner(requireContext());
        String[] stockNames = new String[positionStocks.size()];
        for (int i = 0; i < positionStocks.size(); i++) {
            stockNames[i] = positionStocks.get(i).stockName + " (" + positionStocks.get(i).stockCode + ")";
        }
        ArrayAdapter<String> stockAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, stockNames);
        stockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStock.setAdapter(stockAdapter);
        spinnerStock.setPadding(0, 8, 0, 8);
        layout.addView(spinnerStock);

        TextView labelName = new TextView(requireContext());
        labelName.setText("策略名称");
        labelName.setTextSize(13);
        labelName.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelName.setPadding(0, 12, 0, 0);
        layout.addView(labelName);

        EditText etName = new EditText(requireContext());
        etName.setHint("例如: 突破MA20加仓");
        etName.setTextSize(13);
        etName.setPadding(16, 12, 16, 12);
        etName.setBackgroundResource(R.drawable.bg_input);
        etName.setText(stockNames.length > 0 ? positionStocks.get(0).stockName + "策略" : "策略");
        layout.addView(etName);

        TextView labelCondition = new TextView(requireContext());
        labelCondition.setText("触发条件描述");
        labelCondition.setTextSize(13);
        labelCondition.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelCondition.setPadding(0, 12, 0, 0);
        layout.addView(labelCondition);

        EditText etCondition = new EditText(requireContext());
        etCondition.setHint("例如: 价格上涨突破20日均线");
        etCondition.setTextSize(13);
        etCondition.setPadding(16, 12, 16, 12);
        etCondition.setBackgroundResource(R.drawable.bg_input);
        etCondition.setMinLines(2);
        etCondition.setText(defaultCondition);
        layout.addView(etCondition);

        TextView labelTargetPrice = new TextView(requireContext());
        labelTargetPrice.setText("目标价格（留空则不设置）");
        labelTargetPrice.setTextSize(13);
        labelTargetPrice.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelTargetPrice.setPadding(0, 12, 0, 0);
        layout.addView(labelTargetPrice);

        EditText etTargetPrice = new EditText(requireContext());
        etTargetPrice.setHint("例如: 10.50");
        etTargetPrice.setTextSize(13);
        etTargetPrice.setPadding(16, 12, 16, 12);
        etTargetPrice.setBackgroundResource(R.drawable.bg_input);
        etTargetPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etTargetPrice.setText(defaultTarget);
        layout.addView(etTargetPrice);

        TextView labelStopLoss = new TextView(requireContext());
        labelStopLoss.setText("止损价格（留空则不设置）");
        labelStopLoss.setTextSize(13);
        labelStopLoss.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelStopLoss.setPadding(0, 12, 0, 0);
        layout.addView(labelStopLoss);

        EditText etStopLoss = new EditText(requireContext());
        etStopLoss.setHint("例如: 8.00");
        etStopLoss.setTextSize(13);
        etStopLoss.setPadding(16, 12, 16, 12);
        etStopLoss.setBackgroundResource(R.drawable.bg_input);
        etStopLoss.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etStopLoss.setText(defaultStopLoss);
        layout.addView(etStopLoss);

        TextView labelPriceAbove = new TextView(requireContext());
        labelPriceAbove.setText("价格突破（高于此价触发）");
        labelPriceAbove.setTextSize(13);
        labelPriceAbove.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelPriceAbove.setPadding(0, 12, 0, 0);
        layout.addView(labelPriceAbove);

        EditText etPriceAbove = new EditText(requireContext());
        etPriceAbove.setHint("留空则不设置");
        etPriceAbove.setTextSize(13);
        etPriceAbove.setPadding(16, 12, 16, 12);
        etPriceAbove.setBackgroundResource(R.drawable.bg_input);
        etPriceAbove.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPriceAbove.setText(defaultPriceAbove);
        layout.addView(etPriceAbove);

        TextView labelPriceBelow = new TextView(requireContext());
        labelPriceBelow.setText("价格跌破（低于此价触发）");
        labelPriceBelow.setTextSize(13);
        labelPriceBelow.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelPriceBelow.setPadding(0, 12, 0, 0);
        layout.addView(labelPriceBelow);

        EditText etPriceBelow = new EditText(requireContext());
        etPriceBelow.setHint("留空则不设置");
        etPriceBelow.setTextSize(13);
        etPriceBelow.setPadding(16, 12, 16, 12);
        etPriceBelow.setBackgroundResource(R.drawable.bg_input);
        etPriceBelow.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPriceBelow.setText(defaultPriceBelow);
        layout.addView(etPriceBelow);

        new AlertDialog.Builder(requireContext())
                .setTitle("保存策略")
                .setView(scrollView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "请输入策略名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selIdx = spinnerStock.getSelectedItemPosition();
                    if (selIdx < 0 || selIdx >= positionStocks.size()) return;
                    PositionStock ps = positionStocks.get(selIdx);

                    Strategy strategy = new Strategy();
                    strategy.setStockCode(ps.stockCode);
                    strategy.setStockName(ps.stockName);
                    strategy.setName(name);

                    strategy.setConditionText(etCondition.getText().toString().trim());
                    strategy.setAiMessage(aiContent);

                    try {
                        String tp = etTargetPrice.getText().toString().trim();
                        if (!tp.isEmpty()) strategy.setTargetPrice(Double.parseDouble(tp));
                    } catch (NumberFormatException ignored) {}

                    try {
                        String sl = etStopLoss.getText().toString().trim();
                        if (!sl.isEmpty()) strategy.setStopLossPrice(Double.parseDouble(sl));
                    } catch (NumberFormatException ignored) {}

                    try {
                        String pa = etPriceAbove.getText().toString().trim();
                        if (!pa.isEmpty()) strategy.setConditionPriceAbove(Double.parseDouble(pa));
                    } catch (NumberFormatException ignored) {}

                    try {
                        String pb = etPriceBelow.getText().toString().trim();
                        if (!pb.isEmpty()) strategy.setConditionPriceBelow(Double.parseDouble(pb));
                    } catch (NumberFormatException ignored) {}

                    strategy.setActive(true);

                    StrategyManager.getInstance(requireContext()).saveStrategy(strategy);
                    StrategyAlarmScheduler.scheduleAlarms(requireContext());
                    Toast.makeText(requireContext(), "策略【" + name + "】已保存并开始监控", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String extractConditionText(String text, String signal) {
        if (text == null || text.isEmpty()) return "";

        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^[#*\\-\\d\\.\\s]+", "").trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("```") || trimmed.startsWith("---")) continue;

            for (String kw : new String[]{signal, "目标", "止损", "突破", "跌破", "均线", "MA", "K线", "成交量", "量比", "建议", "操作"}) {
                if (trimmed.contains(kw)) {
                    if (sb.length() > 0) sb.append("；");
                    sb.append(trimmed);
                    break;
                }
            }

            if (sb.length() > 200) break;
        }

        if (sb.length() == 0) {
            String firstLine = lines.length > 0 ? lines[0].replaceAll("^[#*\\-\\s]+", "").trim() : "";
            if (firstLine.length() > 200) firstLine = firstLine.substring(0, 200) + "...";
            return firstLine;
        }

        return sb.toString();
    }

    private String extractPrice(String text, String keyword) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            keyword + "\\s*[：:]\\s*(\\d+\\.?\\d*)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);

        p = java.util.regex.Pattern.compile(
            keyword + "价?\\s*(?:为|到|至|在|约|≈|~|附近)?\\s*(\\d+\\.?\\d{1,3})(?!\\s*(?:日|均|周|月))");
        m = p.matcher(text);
        while (m.find()) {
            String num = m.group(1);
            if (num.indexOf('.') < 0) {
                try {
                    int val = Integer.parseInt(num);
                    if (val >= 5 && val <= 250) continue;
                } catch (NumberFormatException ignored) {}
            }
            return num;
        }

        return "";
    }

    private String formatVolume(double v) {
        if (v <= 0) return "--";
        if (v >= 10000) return new DecimalFormat("#0.00万手").format(v / 10000);
        return new DecimalFormat("#0手").format(v);
    }

    private String formatVolumeInt(double v) {
        if (v <= 0) return "--";
        if (v >= 10000) return new DecimalFormat("#0万").format(v / 10000);
        return String.valueOf((int) v);
    }

    private String formatAmount(double v) {
        if (v <= 0) return "--";
        if (v >= 10000) return new DecimalFormat("#0.00亿").format(v / 10000);
        return new DecimalFormat("#0.00万").format(v);
    }

    private String formatMarketCap(double v) {
        if (v <= 0) return "--";
        double yi = v / 100000000.0;
        if (yi >= 10000) return new DecimalFormat("#0.000万亿").format(yi / 10000);
        return new DecimalFormat("#0.000亿").format(yi);
    }

    private static class PositionStock {
        String stockCode;
        String stockName;
        String stockType;
        double avgCost;
        int recordCount;
        List<PurchaseRecord> purchaseRecords;
        RealtimeQuote quote;
        List<KlineData> klines;
        List<MinuteLineData> minuteData;
        TextView priceView;
        TextView pnlView;
    }
}
