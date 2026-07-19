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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.ChatHistoryManager;
import com.hong.xin.stock.data.PromptTemplateManager;
import com.hong.xin.stock.data.PurchaseRecordManager;
import com.hong.xin.stock.data.StrategyManager;
import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.data.api.EastMoneyApi;
import com.hong.xin.stock.data.api.StockNewsApi;
import com.hong.xin.stock.data.model.ChatMessage;
import com.hong.xin.stock.data.model.KlineData;
import com.hong.xin.stock.data.model.MinuteLineData;
import com.hong.xin.stock.data.model.PurchaseRecord;
import com.hong.xin.stock.data.model.RealtimeQuote;
import com.hong.xin.stock.data.model.Strategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeepSeekChatActivity extends AppCompatActivity {

    private View contextBar;
    private RecyclerView rvChat;
    private EditText etInput;
    private TextView btnSend;
    private TextView btnNews;
    private ChatAdapter chatAdapter;

    private ChatHistoryManager chatHistoryManager;
    private PromptTemplateManager templateManager;

    private String stockCode;
    private String stockName;
    private String currentPrice;
    private String changeText;
    private String stockType;

    private RealtimeQuote quote;
    private List<KlineData> dailyKlines;
    private List<KlineData> marketKlines;
    private List<MinuteLineData> minuteData;
    private boolean dataLoaded = false;
    private boolean loading = false;
    private String customSystemPrompt;
    private String commonSystemPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deepseek_chat);

        Intent intent = getIntent();
        stockCode = intent.getStringExtra("code");
        stockName = intent.getStringExtra("name");
        currentPrice = intent.getStringExtra("price");
        changeText = intent.getStringExtra("change");
        stockType = intent.getStringExtra("type");

        chatHistoryManager = ChatHistoryManager.getInstance(this);
        templateManager = PromptTemplateManager.getInstance(this);

        initViews();
        checkToken();
    }

    private void initViews() {
        contextBar = findViewById(R.id.context_bar);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);

        TextView btnTemplates = findViewById(R.id.btn_templates);
        btnNews = findViewById(R.id.btn_news);
        TextView btnClearHistory = findViewById(R.id.btn_clear_history);

        chatAdapter = new ChatAdapter();
        chatAdapter.setOnStrategySaveListener(this::showStrategySaveDialog);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
        btnTemplates.setOnClickListener(v -> showTemplateDialog());
        btnNews.setOnClickListener(v -> fetchAndAppendNews());
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
        String apiKey = DeepSeekApi.getApiKey(this);
        if (TextUtils.isEmpty(apiKey)) {
            new AlertDialog.Builder(this)
                    .setTitle("未配置API Key")
                    .setMessage("请先在设置中配置DeepSeek API Key后再使用AI分析功能。")
                    .setPositiveButton("去设置", (d, w) -> {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("tab", 1);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("返回", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        etInput.setVisibility(View.VISIBLE);
        btnSend.setVisibility(View.VISIBLE);
        loadCommonPrompt();
        loadCustomPrompt();
        loadContextData();
    }

    private void showPromptEditDialog() {
        String currentPrompt = getEffectiveInstruction();

        EditText input = new EditText(this);
        input.setText(currentPrompt);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(10);
        input.setGravity(android.view.Gravity.TOP);
        input.setPadding(24, 24, 24, 24);
        input.setTextSize(13);
        input.setHorizontallyScrolling(false);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("编辑当前股票提示词（策略指令部分）")
                .setMessage("仅编辑分析策略指令，行情数据（K线/价格/持仓等）会自动附加。")
                .setView(scrollView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        customSystemPrompt = null;
                        saveCustomPrompt("");
                        Toast.makeText(this, "已恢复默认提示词", Toast.LENGTH_SHORT).show();
                    } else {
                        customSystemPrompt = text;
                        saveCustomPrompt(text);
                        Toast.makeText(this, "提示词已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("恢复默认", (dialog, which) -> {
                    customSystemPrompt = null;
                    saveCustomPrompt("");
                    Toast.makeText(this, "已恢复默认提示词", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getDefaultInstruction() {
        return "你是一个专业的A股数据以及交易策略超级分析师。请根据以下行情数据并结合你的知识进行分析。\n" +
               "分析可以包括：技术面、基本面、行业趋势、风险提示、市场情绪、主力挖坑策略，当前股票和大盘的联系等。\n" +
               "优先给出结论再展开具体的止盈止损，加仓减仓，入场清仓，对应的策略价格数值及其表格。\n" +
               "回答请使用中文，要求：精简扼要，重点突出，避免冗余。";
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
        sb.append(buildQuoteSection());
        sb.append(buildKlineSection());
        sb.append(buildMinuteSection());
        sb.append(buildMarketKlineSection());
        sb.append(buildPurchaseSection());
        return sb.toString();
    }

    private String buildSystemPrompt() {
        return getEffectiveInstruction() + "\n\n" + buildDataSections();
    }

    private void saveCustomPrompt(String text) {
        getSharedPreferences("deepseek_config", MODE_PRIVATE)
                .edit().putString("custom_prompt_" + stockCode, text).apply();
    }

    private void loadCustomPrompt() {
        customSystemPrompt = getSharedPreferences("deepseek_config", MODE_PRIVATE)
                .getString("custom_prompt_" + stockCode, null);
    }

    private void loadCommonPrompt() {
        commonSystemPrompt = getSharedPreferences("deepseek_config", MODE_PRIVATE)
                .getString("common_prompt", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataLoaded) {
            loadCommonPrompt();
            loadCustomPrompt();
        }
    }

    private void showModelDialog() {
        String[] models = {"deepseek-v4-pro", "deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner", "\u2728 自定义..."};
        int currentIdx = -1;
        String currentModel = DeepSeekApi.getModel(this);
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(currentModel)) {
                currentIdx = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择模型")
                .setSingleChoiceItems(models, Math.max(currentIdx, 0), null)
                .setPositiveButton("确定", (dialog, which) -> {
                    int pos = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (pos == models.length - 1) {
                        showCustomModelDialog();
                    } else if (pos >= 0) {
                        DeepSeekApi.setModel(this, models[pos]);
                        Toast.makeText(this, "已切换为 " + models[pos], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCustomModelDialog() {
        EditText input = new EditText(this);
        input.setHint("输入模型ID");
        input.setPadding(24, 24, 24, 24);
        input.setText(DeepSeekApi.getModel(this));

        new AlertDialog.Builder(this)
                .setTitle("自定义模型")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String model = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(model)) {
                        DeepSeekApi.setModel(this, model);
                        Toast.makeText(this, "已切换为 " + model, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadContextData() {
        if (loading) return;
        loading = true;
        chatAdapter.addMessage(new ChatMessage("assistant", "正在加载行情数据..."));
        rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        final int[] pending = {5};

        EastMoneyApi.fetchRealtime(stockCode, q -> {
            quote = q;
            if (--pending[0] <= 0) onDataReady();
        });

        EastMoneyApi.fetchExtra(stockCode, q -> {
            if (quote == null) {
                quote = q;
            } else {
                quote = new RealtimeQuote.Builder(quote)
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

        EastMoneyApi.fetchKline(stockCode, 120, klines -> {
            dailyKlines = klines != null ? klines : new ArrayList<>();
            if (--pending[0] <= 0) onDataReady();
        });

        EastMoneyApi.fetchMinuteLineDays(stockCode, 20, data -> {
            minuteData = data != null ? data : new ArrayList<>();
            if (--pending[0] <= 0) onDataReady();
        });

        EastMoneyApi.fetchKline("000001", 120, klines -> {
            marketKlines = klines != null ? klines : new ArrayList<>();
            if (--pending[0] <= 0) onDataReady();
        });
    }

    private void onDataReady() {
        dataLoaded = true;

        chatAdapter.getMessages().clear();
        chatAdapter.notifyDataSetChanged();

        List<ChatMessage> saved = chatHistoryManager.getMessages(stockCode);
        if (!saved.isEmpty()) {
            chatAdapter.addMessages(saved);
        } else {
            showWelcomeMessage();
        }
    }

    private void showWelcomeMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("你好！我是AI股票分析助手。\n\n");
        sb.append("已加载").append(stockName).append("(").append(stockCode).append(")");

        if (quote != null) {
            sb.append("的实时行情");
        }
        if (dailyKlines != null && !dailyKlines.isEmpty()) {
            sb.append("、").append(dailyKlines.size()).append("日K线");
        }
        if (minuteData != null && !minuteData.isEmpty()) {
            sb.append("、").append(minuteData.size()).append("条分时");
        }
        if (marketKlines != null && !marketKlines.isEmpty()) {
            sb.append("、大盘").append(marketKlines.size()).append("日K线");
        }
        sb.append("数据。\n\n");
        sb.append("可以点击上方按钮添加上下文信息，或直接提问。");

        ChatMessage welcomeMsg = new ChatMessage("assistant", sb.toString());
        chatAdapter.addMessage(welcomeMsg);
        chatHistoryManager.addMessage(stockCode, welcomeMsg);
    }

    private String buildQuoteSection() {
        if (quote == null) {
            return "股票：" + stockName + "(" + stockCode + ")\n\n";
        }
        DecimalFormat df = new DecimalFormat("#0.000");
        StringBuilder sb = new StringBuilder();
        sb.append("=== 实时行情 ===\n");
        sb.append("名称：").append(quote.getName()).append("\n");
        sb.append("代码：").append(quote.getCode()).append("\n");
        sb.append("最新价：").append(df.format(quote.getPrice()));
        double chg = quote.getChange();
        double pct = quote.getPctChg();
        sb.append("（").append(chg >= 0 ? "+" : "").append(df.format(chg));
        sb.append("，").append(pct >= 0 ? "+" : "").append(df.format(pct)).append("%）\n");
        sb.append("开盘：").append(df.format(quote.getOpen()));
        sb.append("  最高：").append(df.format(quote.getHigh()));
        sb.append("  最低：").append(df.format(quote.getLow()));
        sb.append("  昨收：").append(df.format(quote.getPreClose())).append("\n");
        sb.append("成交量：").append(formatVolume(quote.getVolume()));
        sb.append("  成交额：").append(formatAmount(quote.getAmount()));
        sb.append("  换手率：").append(quote.getTurnoverRate() > 0 ? df.format(quote.getTurnoverRate()) + "%" : "--").append("\n");
        sb.append("市盈率(静态)：").append(quote.getPe() > 0 ? df.format(quote.getPe()) : "--");
        sb.append("  市盈率(TTM)：").append(quote.getPeTTM() > 0 ? df.format(quote.getPeTTM()) : "--");
        sb.append("  市净率：").append(quote.getPb() > 0 ? df.format(quote.getPb()) : "--").append("\n");
        sb.append("总市值：").append(formatMarketCap(quote.getTotalMarketCap()));
        sb.append("  流通市值：").append(formatMarketCap(quote.getCirculatingMarketCap())).append("\n");
        sb.append("量比：").append(quote.getVolumeRatio() > 0 ? df.format(quote.getVolumeRatio()) : "--");
        sb.append("  涨停价：").append(df.format(quote.getLimitUp()));
        sb.append("  跌停价：").append(df.format(quote.getLimitDown())).append("\n");
        sb.append("每股收益：").append(quote.getEps() > 0 ? df.format(quote.getEps()) : "--");
        sb.append("  股息率：").append(quote.getDividendYield() > 0 ? df.format(quote.getDividendYield()) + "%" : "--").append("\n");
        if (quote.getMa5() > 0) {
            sb.append("均线：MA5=").append(df.format(quote.getMa5()));
            sb.append(" MA10=").append(df.format(quote.getMa10()));
            sb.append(" MA20=").append(df.format(quote.getMa20()));
            sb.append(" MA30=").append(df.format(quote.getMa30()));
            sb.append(" MA60=").append(df.format(quote.getMa60())).append("\n");
        }
        if (stockType != null && stockType.equals("etf") && quote.getIopv() > 0) {
            sb.append("IOPV：").append(df.format(quote.getIopv()));
            sb.append("  溢价率：").append(df.format(quote.getPremiumRate())).append("%\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildKlineSection() {
        if (dailyKlines == null || dailyKlines.isEmpty()) return "";

        DecimalFormat df = new DecimalFormat("#0.00");
        DecimalFormat df3 = new DecimalFormat("#0.000");

        StringBuilder sb = new StringBuilder();
        int show = Math.min(dailyKlines.size(), 120);
        sb.append("=== 历史K线（最近").append(show).append("个交易日） ===\n");
        sb.append("日期        开盘     最高     最低     收盘     涨跌幅    成交量(手)\n");

        int start = dailyKlines.size() - show;
        for (int i = start; i < dailyKlines.size(); i++) {
            KlineData k = dailyKlines.get(i);
            sb.append(k.getDate()).append("  ");
            sb.append(df.format(k.getOpen())).append("  ");
            sb.append(df.format(k.getHigh())).append("  ");
            sb.append(df.format(k.getLow())).append("  ");
            sb.append(df3.format(k.getClose())).append("  ");
            double pctChg = k.getPctChg();
            sb.append(String.format(Locale.getDefault(), "%+.2f%%", pctChg)).append("  ");
            sb.append(formatVolumeInt(k.getVolume())).append("\n");
        }

        if (!dailyKlines.isEmpty()) {
            KlineData last = dailyKlines.get(dailyKlines.size() - 1);
            double highest = last.getHigh();
            double lowest = last.getLow();
            for (int i = start; i < dailyKlines.size(); i++) {
                KlineData k = dailyKlines.get(i);
                if (k.getHigh() > highest) highest = k.getHigh();
                if (k.getLow() < lowest) lowest = k.getLow();
            }
            sb.append("\n").append(show).append("日区间：最高 ").append(df3.format(highest));
            sb.append("  最低 ").append(df3.format(lowest));
            sb.append("  幅度 ").append(String.format(Locale.getDefault(), "%.2f%%", (highest - lowest) / lowest * 100)).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildMinuteSection() {
        if (minuteData == null || minuteData.isEmpty()) return "";

        DecimalFormat df = new DecimalFormat("#0.000");

        double high = 0, low = Double.MAX_VALUE;
        for (MinuteLineData d : minuteData) {
            double p = d.getPrice();
            if (p > 0) {
                if (p > high) high = p;
                if (p < low) low = p;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 20日分时数据概况 ===\n");
        sb.append("最高：").append(df.format(high));
        sb.append("  最低：").append(df.format(low));
        sb.append("  数据条数：").append(minuteData.size()).append("\n\n");

        int detailDays = 2;
        String today = "";
        if (!minuteData.isEmpty()) {
            String t = minuteData.get(minuteData.size() - 1).getTime();
            if (t.length() >= 10) today = t.substring(0, 10);
        }
        sb.append("最近2日分时明细（时间,价格,均价,成交量）：\n");
        int shown = 0;
        for (int i = minuteData.size() - 1; i >= 0; i--) {
            MinuteLineData d = minuteData.get(i);
            String date = d.getTime().length() >= 10 ? d.getTime().substring(0, 10) : "";
            if (!date.equals(today)) {
                today = date;
                shown++;
                if (shown > detailDays) break;
            }
            sb.append(d.getTime()).append(",");
            sb.append(df.format(d.getPrice())).append(",");
            sb.append(df.format(d.getAvgPrice())).append(",");
            sb.append((int) d.getVolume()).append("\n");
        }
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

    private String buildPurchaseSection() {
        List<PurchaseRecord> records = PurchaseRecordManager.getInstance(this).getRecords(stockCode);
        if (records.isEmpty()) return "";
        DecimalFormat df = new DecimalFormat("#0.000");
        StringBuilder sb = new StringBuilder();
        sb.append("=== 用户持仓信息 ===\n");

        double totalCost = 0;
        for (PurchaseRecord r : records) totalCost += r.getPrice();
        double avgCost = totalCost / records.size();

        sb.append("买入均价：").append(df.format(avgCost));
        sb.append("  买入笔数：").append(records.size()).append("\n");
        sb.append("买入记录：");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(df.format(records.get(i).getPrice())).append("@").append(records.get(i).getDate());
        }
        sb.append("\n");

        if (quote != null && quote.getPrice() > 0) {
            double cur = quote.getPrice();
            double pnl = cur - avgCost;
            double pnlPct = (pnl / avgCost) * 100;
            sb.append("当前价格：").append(df.format(cur)).append("\n");
            sb.append("浮动盈亏：").append(String.format(Locale.getDefault(), "%.3f", pnl))
                    .append("（").append(String.format(Locale.getDefault(), "%.3f%%", pnlPct)).append("）\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private void appendContext(String text) {
        String current = etInput.getText().toString();
        if (!TextUtils.isEmpty(current)) {
            current += "\n\n";
        }
        current += text;
        etInput.setText(current);
        etInput.setSelection(current.length());
        Toast.makeText(this, "已添加到输入框，可编辑后发送", Toast.LENGTH_SHORT).show();
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

        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setScrollbarFadingEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择Prompt模板")
                .setView(listView)
                .setNegativeButton("关闭", null)
                .show();

        listView.setOnItemClickListener((parent, view, which, id) -> {
            dialog.dismiss();
            if (which < names.size()) {
                appendContext(templateManager.getTemplate(names.get(which)));
            } else {
                showTemplateEditDialog("", "");
            }
        });
    }

    private void showTemplateEditDialog(String name, String content) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_template_edit, null);
        EditText etName = view.findViewById(R.id.et_template_name);
        EditText etContent = view.findViewById(R.id.et_template_content);

        if (!TextUtils.isEmpty(name)) etName.setText(name);
        if (!TextUtils.isEmpty(content)) etContent.setText(content);

        new AlertDialog.Builder(this)
                .setTitle(TextUtils.isEmpty(name) ? "新建模板" : "编辑模板")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newContent = etContent.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "请输入模板名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(newContent)) {
                        Toast.makeText(this, "请输入模板内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!TextUtils.isEmpty(name) && !name.equals(newName)) {
                        templateManager.deleteTemplate(name);
                    }
                    templateManager.saveTemplate(newName, newContent);
                    Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("清空对话")
                .setMessage("确定要清空当前股票的所有对话记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    chatHistoryManager.clearHistory(stockCode);
                    chatAdapter.getMessages().clear();
                    chatAdapter.notifyDataSetChanged();
                    showWelcomeMessage();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showStrategySaveDialog(ChatMessage message) {
        String aiContent = message.getContent();
        String defaultName = stockName + "策略";
        String defaultTarget = extractPrice(aiContent, "目标");
        String defaultStopLoss = extractPrice(aiContent, "止损");

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
        etName.setHint("例如: 突破阻力位止盈");
        etName.setTextSize(13);
        etName.setPadding(16, 12, 16, 12);
        etName.setBackgroundResource(R.drawable.bg_input);
        etName.setText(defaultName);
        layout.addView(etName);

        TextView labelTargetPrice = new TextView(this);
        labelTargetPrice.setText("止盈价格（价格高于此值触发）");
        labelTargetPrice.setTextSize(13);
        labelTargetPrice.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelTargetPrice.setPadding(0, 12, 0, 0);
        layout.addView(labelTargetPrice);

        EditText etTargetPrice = new EditText(this);
        etTargetPrice.setHint("例如: 10.50");
        etTargetPrice.setTextSize(13);
        etTargetPrice.setPadding(16, 12, 16, 12);
        etTargetPrice.setBackgroundResource(R.drawable.bg_input);
        etTargetPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etTargetPrice.setText(defaultTarget);
        layout.addView(etTargetPrice);

        TextView labelStopLoss = new TextView(this);
        labelStopLoss.setText("止损价格（价格低于此值触发）");
        labelStopLoss.setTextSize(13);
        labelStopLoss.setTextColor(getResources().getColor(R.color.text_primary, null));
        labelStopLoss.setPadding(0, 12, 0, 0);
        layout.addView(labelStopLoss);

        EditText etStopLoss = new EditText(this);
        etStopLoss.setHint("例如: 8.00");
        etStopLoss.setTextSize(13);
        etStopLoss.setPadding(16, 12, 16, 12);
        etStopLoss.setBackgroundResource(R.drawable.bg_input);
        etStopLoss.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etStopLoss.setText(defaultStopLoss);
        layout.addView(etStopLoss);

        new AlertDialog.Builder(this)
                .setTitle("保存策略")
                .setView(scrollView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "请输入策略名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Strategy strategy = new Strategy();
                    strategy.setStockCode(stockCode);
                    strategy.setStockName(stockName);
                    strategy.setName(name);
                    strategy.setAiMessage(aiContent);

                    try {
                        String tp = etTargetPrice.getText().toString().trim();
                        if (!tp.isEmpty()) strategy.setTargetPrice(Double.parseDouble(tp));
                    } catch (NumberFormatException ignored) {}

                    try {
                        String sl = etStopLoss.getText().toString().trim();
                        if (!sl.isEmpty()) strategy.setStopLossPrice(Double.parseDouble(sl));
                    } catch (NumberFormatException ignored) {}

                    strategy.setActive(true);

                    StrategyManager.getInstance(this).saveStrategy(strategy);
                    StrategyAlarmScheduler.scheduleAlarms(this);
                    Toast.makeText(this, "策略【" + name + "】已保存并开始监控", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setSpinnerByValue(Spinner spinner, String[] options, String value) {
        if (value == null || value.isEmpty()) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String detectSignalType(String text) {
        String[][] pairs = {
            {"加仓", "加仓"}, {"减仓", "减仓"}, {"止盈", "止盈"}, {"止损", "止损"},
            {"入场", "入场"}, {"退场", "退场"}, {"买入", "买入"}, {"卖出", "卖出"},
            {"建仓", "入场"}, {"平仓", "退场"}, {"抄底", "入场"}, {"逃顶", "退场"},
            {"突破买入", "买入"}, {"逢低买入", "买入"}, {"高抛", "卖出"},
            {"低吸", "买入"}, {"加码", "加仓"}, {"减码", "减仓"},
            {"做多", "入场"}, {"做空", "退场"}, {"离场", "退场"}
        };
        for (String[] pair : pairs) {
            if (text.contains(pair[0])) return pair[1];
        }
        return "入场";
    }

    private String detectDirection(String text) {
        if (text.contains("看多") || text.contains("做多") || text.contains("上涨") ||
                text.contains("突破") || text.contains("反弹") || text.contains("抄底")) {
            return "看多";
        }
        if (text.contains("看空") || text.contains("做空") || text.contains("下跌") ||
                text.contains("跌破") || text.contains("见顶") || text.contains("逃顶")) {
            return "看空";
        }
        return "";
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
                    if (val >= 5 && val <= 250) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {}
            }
            return num;
        }

        return "";
    }

    private int extractMaCondition(String text) {
        int[] mas = {60, 30, 20, 10, 5};
        for (int ma : mas) {
            if (text.contains("MA" + ma) || text.contains("ma" + ma) ||
                    text.contains(ma + "日线") || text.contains(ma + "日均线") ||
                    text.contains(ma + "日均")) {
                return ma;
            }
        }
        return 0;
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

    private double extractVolumeRatioCondition(String text) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "量比\\s*(?:[：:>大于超过高])\\s*(\\d+\\.?\\d*)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return Double.parseDouble(m.group(1));
        return 0;
    }

    private void fetchAndAppendNews() {
        Toast.makeText(this, "正在获取新闻...", Toast.LENGTH_SHORT).show();

        final List<StockNewsApi.NewsItem>[] newsRef = new List[1];
        final int[] pending = {1};

        StockNewsApi.fetchStockNews(stockCode, stockName, 8, items -> {
            if (items != null) {
                newsRef[0] = items;
            }
            if (--pending[0] <= 0) showNewsDialog(newsRef[0]);
        });
    }

    private void showNewsDialog(List<StockNewsApi.NewsItem> news) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== ").append(stockName).append(" 最新新闻 =====\n\n");

        if (news != null && !news.isEmpty()) {
            for (StockNewsApi.NewsItem item : news) {
                String date = item.getDate();
                if (!TextUtils.isEmpty(date) && date.length() > 10) date = date.substring(0, 10);
                sb.append("• [").append(date).append("] ").append(item.getTitle()).append("\n");
            }
        } else {
            sb.append("暂无相关新闻数据。");
        }

        String newsText = sb.toString();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scrollView.setScrollbarFadingEnabled(false);

        TextView tv = new TextView(this);
        tv.setText(newsText);
        tv.setTextSize(13);
        tv.setPadding(24, 16, 24, 16);
        tv.setTextIsSelectable(true);
        scrollView.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("最新相关新闻")
                .setView(scrollView)
                .setPositiveButton("确定", (d, w) -> appendContext(newsText))
                .setNegativeButton("取消", null)
                .show();
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String apiKey = DeepSeekApi.getApiKey(this);
        if (TextUtils.isEmpty(apiKey)) {
            checkToken();
            return;
        }

        etInput.setText("");

        ChatMessage userMsg = new ChatMessage("user", text);
        chatAdapter.addMessage(userMsg);
        chatHistoryManager.addMessage(stockCode, userMsg);

        ChatMessage loadingMsg = new ChatMessage("assistant", "⏳ 正在分析...");
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

        DeepSeekApi.chat(this, apiMessages, new DeepSeekApi.ChatCallback() {
            @Override
            public void onResult(String content) {
                runOnUiThread(() -> {
                    List<ChatMessage> msgs = chatAdapter.getMessages();
                    msgs.remove(msgs.size() - 1);
                    chatAdapter.notifyItemRemoved(msgs.size());

                    ChatMessage assistantMsg = new ChatMessage("assistant", content);
                    chatAdapter.addMessage(assistantMsg);
                    chatHistoryManager.addMessage(stockCode, assistantMsg);
                    rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    btnSend.setEnabled(true);
                    btnSend.setText("发送");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    List<ChatMessage> msgs = chatAdapter.getMessages();
                    msgs.remove(msgs.size() - 1);
                    chatAdapter.notifyItemRemoved(msgs.size());
                    Toast.makeText(DeepSeekChatActivity.this, error, Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                    btnSend.setText("发送");
                });
            }
        });
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
}
