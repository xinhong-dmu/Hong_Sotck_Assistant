package com.hong.xin.stock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.data.PromptTemplateManager;

import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private LinearLayout container;
    private PromptTemplateManager templateManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_settings, parent, false);
        container = view.findViewById(R.id.settings_container);
        templateManager = PromptTemplateManager.getInstance(requireContext());

        buildSettings();
        return view;
    }

    private void addSectionTitle(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        tv.setPadding(0, 24, 0, 8);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(tv);
    }

    private void addClickableItem(String text, String value, int color, Runnable onClick) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOnClickListener(v -> onClick.run());

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 4, 0, 4);
        row.setLayoutParams(rowLp);

        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(getResources().getColor(R.color.text_primary, null));
        label.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label);

        if (value != null) {
            TextView val = new TextView(requireContext());
            val.setText(value);
            val.setTextSize(13);
            val.setTextColor(color);
            val.setPadding(16, 0, 0, 0);
            row.addView(val);
        }

        container.addView(row);
    }

    private void buildSettings() {
        addSectionTitle("DeepSeek API");
        addClickableItem("配置API Key和模型", DeepSeekApi.getModel(requireContext()), getResources().getColor(R.color.green, null),
                this::showApiDialog);

        addSectionTitle("策略提示词");
        addClickableItem("编辑通用Prompt", null, getResources().getColor(R.color.orange, null),
                this::showCommonPromptDialog);

        addSectionTitle("对话模板");
        addClickableItem("管理对话模板", null, getResources().getColor(R.color.primary, null),
                this::showTemplateManageDialog);

        addSectionTitle("策略管理");
        addClickableItem("进入策略管理", null, getResources().getColor(R.color.primary, null), () -> {
            startActivity(new Intent(requireActivity(), StrategyListActivity.class));
        });
    }

    private void showApiDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText etApiKey = new EditText(requireContext());
        etApiKey.setHint("sk-...");
        etApiKey.setTextSize(13);
        etApiKey.setPadding(16, 12, 16, 12);
        etApiKey.setBackgroundResource(R.drawable.bg_input);
        etApiKey.setText(DeepSeekApi.getApiKey(requireContext()));
        layout.addView(etApiKey);

        TextView labelModel = new TextView(requireContext());
        labelModel.setText("默认模型");
        labelModel.setTextSize(13);
        labelModel.setTextColor(getResources().getColor(R.color.text_secondary, null));
        labelModel.setPadding(0, 12, 0, 4);
        layout.addView(labelModel);

        Spinner spinnerModel = new Spinner(requireContext());
        String[] models = {"deepseek-v4-pro", "deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
        spinnerModel.setPadding(0, 8, 0, 8);
        spinnerModel.setBackgroundResource(R.drawable.bg_input);

        String currentModel = DeepSeekApi.getModel(requireContext());
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(currentModel)) {
                spinnerModel.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerModel);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 16, 0, 0);

        TextView btnSave = new TextView(requireContext());
        btnSave.setText("保存配置");
        btnSave.setTextSize(14);
        btnSave.setTextColor(getResources().getColor(R.color.white, null));
        btnSave.setPadding(24, 10, 24, 10);
        btnSave.setBackgroundResource(R.drawable.bg_primary_button);
        btnSave.setGravity(android.view.Gravity.CENTER);
        btnRow.addView(btnSave);

        TextView btnClear = new TextView(requireContext());
        btnClear.setText("清除Key");
        btnClear.setTextSize(14);
        btnClear.setTextColor(getResources().getColor(R.color.red, null));
        btnClear.setPadding(24, 10, 24, 10);
        btnClear.setBackgroundResource(R.drawable.bg_outlined_button);
        btnClear.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clearLp.setMargins(12, 0, 0, 0);
        btnClear.setLayoutParams(clearLp);
        btnRow.addView(btnClear);

        layout.addView(btnRow);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("DeepSeek API 配置")
                .setView(layout)
                .setNegativeButton("关闭", null)
                .show();

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (TextUtils.isEmpty(key)) {
                etApiKey.setError("请输入API Key");
                return;
            }
            DeepSeekApi.setApiKey(requireContext(), key);
            DeepSeekApi.setModel(requireContext(), spinnerModel.getSelectedItem().toString());
            Toast.makeText(requireContext(), "配置已保存", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            container.removeAllViews();
            buildSettings();
        });

        btnClear.setOnClickListener(v -> {
            DeepSeekApi.setApiKey(requireContext(), "");
            etApiKey.setText("");
            Toast.makeText(requireContext(), "API Key已清除", Toast.LENGTH_SHORT).show();
        });
    }

    private void showCommonPromptDialog() {
        String current = requireContext().getSharedPreferences("deepseek_config", 0)
                .getString("common_prompt", null);

        String defaultInstruction = "你是一个专业的A股数据以及交易策略超级分析师。请根据以下行情数据并结合你的知识进行分析。\n" +
                "分析可以包括：技术面、基本面、行业趋势、风险提示、市场情绪、主力挖坑策略，当前股票和大盘的联系等。\n" +
                "优先给出结论再展开具体的止盈止损，加仓减仓，入场清仓，对应的策略价格数值及其表格。\n" +
                "回答请使用中文，要求：精简扼要，重点突出，避免冗余。";

        EditText input = new EditText(requireContext());
        input.setText(!TextUtils.isEmpty(current) ? current : defaultInstruction);
        input.setMinLines(8);
        input.setGravity(android.view.Gravity.TOP);
        input.setPadding(24, 24, 24, 24);
        input.setTextSize(13);
        input.setHorizontallyScrolling(false);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑通用Prompt（策略指令部分）")
                .setMessage("仅编辑分析策略指令，行情数据（K线/价格/持仓等）会自动附加。")
                .setView(scrollView)
                .setPositiveButton("保存", (d, w) -> {
                    String text = input.getText().toString().trim();
                    requireContext().getSharedPreferences("deepseek_config", 0)
                            .edit().putString("common_prompt", text).apply();
                    Toast.makeText(requireContext(),
                            TextUtils.isEmpty(text) ? "已清除通用Prompt" : "通用Prompt已保存",
                            Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("恢复默认", (d, w) -> {
                    requireContext().getSharedPreferences("deepseek_config", 0)
                            .edit().putString("common_prompt", "").apply();
                    Toast.makeText(requireContext(), "已恢复默认策略指令", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTemplateManageDialog() {
        Map<String, String> templates = templateManager.getTemplates();
        List<String> names = templateManager.getTemplateNames();

        String[] items;
        if (names.isEmpty()) {
            items = new String[]{"+ 新建模板"};
        } else {
            items = new String[names.size() + 1];
            for (int i = 0; i < names.size(); i++) items[i] = names.get(i);
            items[names.size()] = "+ 新建模板";
        }

        ListView listView = new ListView(requireContext());
        listView.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items));
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setScrollbarFadingEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("对话模板管理")
                .setView(listView)
                .setNegativeButton("关闭", null)
                .show();

        listView.setOnItemClickListener((parent, view, which, id) -> {
            dialog.dismiss();
            if (which < names.size()) {
                showTemplateActionDialog(names.get(which));
            } else {
                showTemplateEditDialog("", "");
            }
        });
    }

    private void showTemplateActionDialog(String name) {
        String content = templateManager.getTemplate(name);
        new AlertDialog.Builder(requireContext())
                .setTitle(name)
                .setMessage(content)
                .setNeutralButton("编辑", (dialog, which) -> showTemplateEditDialog(name, content))
                .setNegativeButton("删除", (dialog, which) -> {
                    templateManager.deleteTemplate(name);
                    Toast.makeText(requireContext(), "模板已删除", Toast.LENGTH_SHORT).show();
                })
                .show();
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
}
