package com.hong.xin.stock.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.TradePreset;
import com.hong.xin.stock.util.SettingsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private SettingsManager settingsManager;
    private LinearLayout presetContainer;
    private TextView emptyHint;
    private FloatingActionButton fabNewTrade;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsManager = new SettingsManager(requireContext());
        presetContainer = view.findViewById(R.id.preset_container);
        emptyHint = view.findViewById(R.id.empty_hint);
        fabNewTrade = view.findViewById(R.id.fab_new_trade);

        fabNewTrade.setOnClickListener(v -> {
            settingsManager.clearDashboard();
            settingsManager.saveTradeParams("", "", 0, "", 3, 15, 0, false);
            switchToTradeTab();
        });

        refreshPresets();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPresets();
    }

    private void refreshPresets() {
        List<TradePreset> presets = settingsManager.getPresets();
        presetContainer.removeAllViews();

        if (presets.isEmpty()) {
            emptyHint.setVisibility(View.VISIBLE);
            return;
        }

        emptyHint.setVisibility(View.GONE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (TradePreset preset : presets) {
            CardView card = (CardView) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_preset, presetContainer, false);

            TextView titleText = card.findViewById(R.id.preset_title);
            TextView subtitleText = card.findViewById(R.id.preset_subtitle);
            TextView paramsText = card.findViewById(R.id.preset_params);
            TextView timeText = card.findViewById(R.id.preset_time);
            Button loadBtn = card.findViewById(R.id.preset_load_btn);
            Button deleteBtn = card.findViewById(R.id.preset_delete_btn);

            titleText.setText(preset.getDisplayTitle());
            subtitleText.setText(preset.getDisplaySubtitle());
            paramsText.setText(preset.getDisplayParams());
            timeText.setText("保存时间: " + preset.getSavedAt());

            loadBtn.setOnClickListener(v -> {
                settingsManager.clearDashboard();
                settingsManager.loadPresetParams(preset);
                switchToTradeTab();
            });

            deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除 \"" + preset.getDisplayTitle() + "\" 的交易参数吗?")
                        .setPositiveButton("删除", (dialog, which) -> {
                            settingsManager.deletePreset(preset.getId());
                            refreshPresets();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });

            presetContainer.addView(card);
        }
    }

    private void switchToTradeTab() {
        if (getActivity() instanceof com.hong.xin.stock.MainActivity) {
            ((com.hong.xin.stock.MainActivity) getActivity()).switchToTradeTab();
        }
    }
}
