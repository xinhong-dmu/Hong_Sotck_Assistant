package com.hong.xin.stock.ui.search;

import com.hong.xin.stock.data.model.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StockSearchUiState {

    private final Stock selectedStock;
    private final boolean isAnalyzing;
    private final List<ChatMessage> messages;

    private StockSearchUiState(Builder builder) {
        this.selectedStock = builder.selectedStock;
        this.isAnalyzing = builder.isAnalyzing;
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
    }

    public Stock getSelectedStock() { return selectedStock; }
    public boolean isAnalyzing() { return isAnalyzing; }
    public List<ChatMessage> getMessages() { return messages; }

    public static Builder builder() { return new Builder(); }

    public Builder copy() {
        return new Builder()
                .selectedStock(selectedStock)
                .isAnalyzing(isAnalyzing)
                .messages(messages);
    }

    public static class ChatMessage {
        public final boolean isUser;
        public final String content;

        public ChatMessage(boolean isUser, String content) {
            this.isUser = isUser;
            this.content = content;
        }
    }

    public static class Builder {
        private Stock selectedStock;
        private boolean isAnalyzing = false;
        private List<ChatMessage> messages = new ArrayList<>();

        public Builder selectedStock(Stock v) { this.selectedStock = v; return this; }
        public Builder isAnalyzing(boolean v) { this.isAnalyzing = v; return this; }
        public Builder messages(List<ChatMessage> v) { this.messages = new ArrayList<>(v); return this; }

        public StockSearchUiState build() { return new StockSearchUiState(this); }
    }
}
