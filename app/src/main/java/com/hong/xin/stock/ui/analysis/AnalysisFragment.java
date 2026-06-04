package com.hong.xin.stock.ui.analysis;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.hong.xin.stock.util.SettingsManager;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class AnalysisFragment extends Fragment {

    private AiAnalysisViewModel viewModel;
    private SettingsManager settingsManager;

    private Button stopAnalysisBtn, clearChatBtn;
    private ScrollView chatScroll;
    private LinearLayout chatContainer;
    private EditText chatInput;
    private Button sendBtn;

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
        settingsManager = new SettingsManager(requireContext());

        initViews(view);
        setupListeners();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (settingsManager.isAnalysisRequested()) {
            settingsManager.setAnalysisRequested(false);
            viewModel.loadFromSettings();
            viewModel.clearChat();
            viewModel.startAnalysis();
        }
    }

    private void initViews(View view) {
        stopAnalysisBtn = view.findViewById(R.id.stop_analysis_btn);
        clearChatBtn = view.findViewById(R.id.clear_chat_btn);
        chatScroll = view.findViewById(R.id.chat_scroll);
        chatContainer = view.findViewById(R.id.chat_container);
        chatInput = view.findViewById(R.id.chat_input);
        sendBtn = view.findViewById(R.id.send_btn);
    }

    private void setupListeners() {
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
            if (msg.isUser) {
                tv.setText(msg.content);
            } else {
                tv.setText(renderMarkdown(msg.content));
            }
            chatContainer.addView(bubble);
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private Spanned renderMarkdown(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }
}
