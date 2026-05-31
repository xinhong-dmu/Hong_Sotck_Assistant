package com.hong.xin.stock.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.api.DeepSeekApi;
import com.hong.xin.stock.util.SettingsManager;

public class SettingsFragment extends Fragment {

    private EditText apiKeyInput;
    private Spinner modelSpinner;
    private Button saveBtn;
    private Button getApiKeyBtn;
    private TextView saveHint;
    private SettingsManager settingsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsManager = new SettingsManager(requireContext());

        apiKeyInput = view.findViewById(R.id.settings_api_key);
        modelSpinner = view.findViewById(R.id.model_spinner);
        saveBtn = view.findViewById(R.id.save_settings_btn);
        getApiKeyBtn = view.findViewById(R.id.get_api_key_btn);
        saveHint = view.findViewById(R.id.save_hint);

        setupModelSpinner();
        loadSettings();
        setupApiKeyButton();
        setupListeners();
    }

    private void setupModelSpinner() {
        String[] items = {"DeepSeek V4 Pro", "DeepSeek Flash"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
    }

    private void loadSettings() {
        String savedKey = settingsManager.getApiKey();
        if (!savedKey.isEmpty()) {
            apiKeyInput.setText(savedKey);
        }

        String savedModel = settingsManager.getDeepSeekModel();
        int position = getModelPosition(savedModel);
        modelSpinner.setSelection(position);
    }

    private int getModelPosition(String model) {
        if ("deepseek-reasoner".equals(model)) return 1;
        return 0;
    }

    private void setupListeners() {
        saveBtn.setOnClickListener(v -> {
            String key = apiKeyInput.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(requireContext(), "请输入 API Key", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                settingsManager.setApiKey(key);
                DeepSeekApi.setApiKey(key);

                String savedKey = settingsManager.getApiKey();
                if (!savedKey.equals(key)) {
                    saveHint.setText("保存失败：API Key 读写不一致");
                    saveHint.setTextColor(0xFFF44336);
                    saveHint.setVisibility(View.VISIBLE);
                    saveHint.postDelayed(() -> saveHint.setVisibility(View.GONE), 3000);
                    return;
                }

                int selected = modelSpinner.getSelectedItemPosition();
                String model = getModelValue(selected);
                settingsManager.setDeepSeekModel(model);
                DeepSeekApi.setModel(model);

                saveHint.setText("保存成功！API Key 和模型设置已生效");
                saveHint.setTextColor(0xFF4CAF50);
                saveHint.setVisibility(View.VISIBLE);
                saveHint.postDelayed(() -> saveHint.setVisibility(View.GONE), 3000);
            } catch (Exception e) {
                saveHint.setText("保存失败：" + e.getMessage());
                saveHint.setTextColor(0xFFF44336);
                saveHint.setVisibility(View.VISIBLE);
                saveHint.postDelayed(() -> saveHint.setVisibility(View.GONE), 3000);
            }
        });
    }

    private void setupApiKeyButton() {
        updateGetApiKeyButtonVisibility();

        apiKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateGetApiKeyButtonVisibility();
            }
        });

        getApiKeyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://platform.deepseek.com"));
            startActivity(intent);
        });
    }

    private void updateGetApiKeyButtonVisibility() {
        String key = apiKeyInput.getText().toString().trim();
        getApiKeyBtn.setVisibility(key.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getModelValue(int position) {
        if (position == 1) {
            return "deepseek-reasoner";
        }
        return "deepseek-chat";
    }
}
