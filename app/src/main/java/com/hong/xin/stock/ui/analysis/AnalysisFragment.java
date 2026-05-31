package com.hong.xin.stock.ui.analysis;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hong.xin.stock.R;
import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.ui.widget.StockFilterAdapter;

public class AnalysisFragment extends Fragment {

    private AiAnalysisViewModel viewModel;

    private AutoCompleteTextView searchEdit;
    private EditText stockCode, stockName, buyPriceAnalysis, buyDateAnalysis, klineDays;
    private EditText stopLossPercent, targetProfitPercent;
    private Button startAnalysisBtn, stopAnalysisBtn, clearChatBtn;
    private ScrollView chatScroll;
    private LinearLayout chatContainer;
    private EditText chatInput;
    private Button sendBtn;
    private View analysisPanel;

    private StockRepository stockRepository;
    private StockFilterAdapter searchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AiAnalysisViewModel.class);
        stockRepository = StockRepository.getInstance();

        initViews(view);
        setupAutoComplete();
        setupListeners();
        observeViewModel();
        setupKeyboardScroll();
    }

    private void initViews(View view) {
        searchEdit = view.findViewById(R.id.search_edit);
        stockCode = view.findViewById(R.id.stock_code);
        stockName = view.findViewById(R.id.stock_name);
        buyPriceAnalysis = view.findViewById(R.id.buy_price_analysis);
        buyDateAnalysis = view.findViewById(R.id.buy_date_analysis);
        klineDays = view.findViewById(R.id.kline_days);
        stopLossPercent = view.findViewById(R.id.stop_loss_percent);
        targetProfitPercent = view.findViewById(R.id.target_profit_percent);
        buyPriceAnalysis.setVisibility(View.GONE);
        buyDateAnalysis.setVisibility(View.GONE);
        stopLossPercent.setVisibility(View.GONE);
        targetProfitPercent.setVisibility(View.GONE);
        startAnalysisBtn = view.findViewById(R.id.start_analysis_btn);
        stopAnalysisBtn = view.findViewById(R.id.stop_analysis_btn);
        clearChatBtn = view.findViewById(R.id.clear_chat_btn);
        chatScroll = view.findViewById(R.id.chat_scroll);
        chatContainer = view.findViewById(R.id.chat_container);
        chatInput = view.findViewById(R.id.chat_input);
        sendBtn = view.findViewById(R.id.send_btn);
        analysisPanel = view.findViewById(R.id.analysis_panel);
    }

    private void setupAutoComplete() {
        searchAdapter = new StockFilterAdapter(requireContext());
        searchEdit.setThreshold(1);
        searchEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchEdit.setAdapter(searchAdapter);

        searchEdit.setOnItemClickListener((parent, view, position, id) -> {
            Stock selected = searchAdapter.getStock(position);
            if (selected != null) {
                searchEdit.dismissDropDown();
                stockName.setText(selected.getName());
                stockCode.setText(selected.getCode());
                viewModel.updateStockInfo(selected.getCode(), selected.getName());
            }
            searchEdit.setText("");
        });
    }

    private void setupListeners() {
        stockCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateStockInfo();
        });
        stockName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateStockInfo();
        });

        klineDays.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = klineDays.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        int days = Integer.parseInt(text);
                        days = Math.max(5, Math.min(365, days));
                        viewModel.updateKlineDays(days);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });

        startAnalysisBtn.setOnClickListener(v -> {
            updateStockInfo();
            viewModel.startAnalysis();
        });

        stopAnalysisBtn.setOnClickListener(v -> viewModel.stopAnalysis());

        clearChatBtn.setOnClickListener(v -> viewModel.clearChat());

        sendBtn.setOnClickListener(v -> {
            String text = chatInput.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                chatInput.setText("");
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            stockCode.setText(state.getStockCode());
            stockName.setText(state.getStockName());
            klineDays.setText(String.valueOf(state.getKlineDays()));

            startAnalysisBtn.setVisibility(state.isAnalyzing() ? View.GONE : View.VISIBLE);
            stopAnalysisBtn.setVisibility(state.isAnalyzing() ? View.VISIBLE : View.GONE);

            refreshChatMessages(state.getMessages());
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshChatMessages(java.util.List<AiAnalysisState.ChatMessage> messages) {
        chatContainer.removeAllViews();
        for (AiAnalysisState.ChatMessage msg : messages) {
            View bubble;
            if (msg.isUser) {
                bubble = LayoutInflater.from(requireContext()).inflate(R.layout.item_chat_user, chatContainer, false);
            } else {
                bubble = LayoutInflater.from(requireContext()).inflate(R.layout.item_chat_ai, chatContainer, false);
            }
            TextView tv = bubble.findViewById(android.R.id.text1);
            tv.setText(msg.content);
            chatContainer.addView(bubble);
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void setupKeyboardScroll() {
        View root = getView();
        if (root == null) return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            root.getWindowVisibleDisplayFrame(rect);
            int screenHeight = root.getRootView().getHeight();
            int keyboardHeight = screenHeight - rect.bottom;
            if (keyboardHeight > screenHeight * 0.15) {
                analysisPanel.setVisibility(View.GONE);
                chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
            } else {
                analysisPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateStockInfo() {
        String code = stockCode.getText().toString().trim();
        String name = stockName.getText().toString().trim();
        if (!code.isEmpty() && !name.isEmpty()) {
            viewModel.updateStockInfo(code, name);
        }
    }
}
