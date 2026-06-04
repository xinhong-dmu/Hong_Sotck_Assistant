package com.hong.xin.stock.ui.search;

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
import com.hong.xin.stock.util.DebugLogger;

import java.util.ArrayList;
import java.util.List;

public class StockSearchFragment extends Fragment {

    private StockSearchViewModel viewModel;

    private AutoCompleteTextView searchStockEdit;
    private TextView selectedStockInfo;
    private Button aiAnalysisBtn, clearChatBtn;
    private ScrollView chatScroll;
    private LinearLayout chatContainer;
    private LinearLayout inputArea;
    private EditText chatInput;
    private Button sendBtn;
    private TextView emptyHint;
    private View analysisPanel;

    private StockRepository stockRepository;
    private Stock currentSelectedStock;
    private StockFilterAdapter searchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StockSearchViewModel.class);
        stockRepository = StockRepository.getInstance();

        initViews(view);
        setupAutoComplete();
        setupListeners();
        observeViewModel();
        setupKeyboardScroll();
    }

    private void initViews(View view) {
        searchStockEdit = view.findViewById(R.id.search_stock_edit);
        selectedStockInfo = view.findViewById(R.id.selected_stock_info);
        aiAnalysisBtn = view.findViewById(R.id.ai_analysis_btn);
        clearChatBtn = view.findViewById(R.id.clear_chat_btn);
        chatScroll = view.findViewById(R.id.chat_scroll);
        chatContainer = view.findViewById(R.id.chat_container);
        inputArea = view.findViewById(R.id.input_area);
        chatInput = view.findViewById(R.id.chat_input);
        sendBtn = view.findViewById(R.id.send_btn);
        emptyHint = view.findViewById(R.id.empty_hint);
        analysisPanel = view.findViewById(R.id.analysis_panel);
    }

    private void setupAutoComplete() {
        DebugLogger.i("StockSearch", "Stock count in repo: " + stockRepository.getStockCount());
        searchAdapter = new StockFilterAdapter(requireContext());
        searchStockEdit.setThreshold(1);
        searchStockEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchStockEdit.setAdapter(searchAdapter);
        DebugLogger.i("StockSearch", "AutoCompleteTextView adapter set, threshold=1");

        searchStockEdit.setOnItemClickListener((parent, view, position, id) -> {
            Stock selected = searchAdapter.getStock(position);
            if (selected != null) {
                currentSelectedStock = selected;
                String info = "已选: " + selected.getName() + " (" + selected.getCode() + ")";
                if (selected.isEtf()) {
                    info = "[ETF] " + info;
                }
                selectedStockInfo.setText(info);
                selectedStockInfo.setVisibility(View.VISIBLE);
                aiAnalysisBtn.setVisibility(View.VISIBLE);
                viewModel.selectStock(selected);
            }
            searchStockEdit.setText("");
        });
    }

    private void setupListeners() {
        aiAnalysisBtn.setOnClickListener(v -> viewModel.startAiAnalysis());

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

            refreshChatMessages(state.getMessages());
            boolean hasMessages = !state.getMessages().isEmpty();
            boolean hasStock = state.getSelectedStock() != null;

            chatScroll.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
            inputArea.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
            emptyHint.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
            clearChatBtn.setVisibility(hasMessages ? View.VISIBLE : View.GONE);

            if (state.isAnalyzing()) {
                aiAnalysisBtn.setEnabled(false);
                aiAnalysisBtn.setText("分析中...");
            } else {
                aiAnalysisBtn.setEnabled(true);
                aiAnalysisBtn.setText("AI分析此股");
            }

            if (hasStock && state.getSelectedStock() != currentSelectedStock) {
                currentSelectedStock = state.getSelectedStock();
                String info = "已选: " + currentSelectedStock.getName() + " (" + currentSelectedStock.getCode() + ")";
                if (currentSelectedStock.isEtf()) {
                    info = "[ETF] " + info;
                }
                selectedStockInfo.setText(info);
                selectedStockInfo.setVisibility(View.VISIBLE);
                aiAnalysisBtn.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });


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

    private void refreshChatMessages(java.util.List<StockSearchUiState.ChatMessage> messages) {
        chatContainer.removeAllViews();
        for (StockSearchUiState.ChatMessage msg : messages) {
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
}
