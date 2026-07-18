package com.hong.xin.stock;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.method.LinkMovementMethod;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hong.xin.stock.data.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public interface OnStrategySaveListener {
        void onSaveStrategy(ChatMessage message);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private Markwon markwon;
    private OnStrategySaveListener strategySaveListener;

    public void setOnStrategySaveListener(OnStrategySaveListener listener) {
        this.strategySaveListener = listener;
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<ChatMessage> msgs) {
        int start = messages.size();
        messages.addAll(msgs);
        notifyItemRangeInserted(start, msgs.size());
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public ChatMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public void appendToLast(String chunk) {
        if (messages.isEmpty()) return;
        ChatMessage last = messages.get(messages.size() - 1);
        last.setContent(last.getContent() + chunk);
    }

    public void renderStreaming(RecyclerView rv) {
        int pos = messages.size() - 1;
        if (pos < 0) return;
        ViewHolder holder = (ViewHolder) rv.findViewHolderForAdapterPosition(pos);
        if (holder != null) {
            holder.tvMessage.setText(messages.get(pos).getContent());
        }
    }

    public void finishStream(RecyclerView rv) {
        int pos = messages.size() - 1;
        if (pos >= 0) {
            notifyItemChanged(pos);
        }
    }

    private boolean containsSignalKeyword(String content) {
        if (content == null) return false;
        Pattern p = Pattern.compile("(加仓|减仓|止盈|止损|入场|出场|退场|买入|卖出|建仓|平仓|突破|抄底|逃顶)");
        return p.matcher(content).find();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        if (markwon == null) {
            markwon = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .usePlugin(StrikethroughPlugin.create())
                    .build();
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        holder.tvMessage.setMovementMethod(null);

        if ("user".equals(msg.getRole())) {
            holder.tvMessage.setText(msg.getContent());
            holder.tvMessage.setBackgroundResource(R.drawable.chart_tab_bg);
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.itemView.setPadding(60, 4, 4, 4);
            holder.btnSaveStrategy.setVisibility(View.GONE);
        } else {
            markwon.setMarkdown(holder.tvMessage, msg.getContent());
            holder.tvMessage.setBackgroundColor(Color.parseColor("#E3F2FD"));
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvMessage.setLinkTextColor(Color.parseColor("#1976D2"));
            holder.tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            holder.itemView.setPadding(4, 4, 60, 4);

            if (containsSignalKeyword(msg.getContent())) {
                holder.btnSaveStrategy.setVisibility(View.VISIBLE);
                holder.btnSaveStrategy.setOnClickListener(v -> {
                    if (strategySaveListener != null) {
                        strategySaveListener.onSaveStrategy(msg);
                    }
                });
            } else {
                holder.btnSaveStrategy.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView btnSaveStrategy;

        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            btnSaveStrategy = itemView.findViewById(R.id.btn_save_strategy);
        }
    }
}
