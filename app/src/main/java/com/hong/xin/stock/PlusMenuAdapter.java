package com.hong.xin.stock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PlusMenuAdapter extends BaseAdapter {

    private final String[] items;
    private final String[] icons;

    public PlusMenuAdapter(String[] items, String[] icons) {
        this.items = items;
        this.icons = icons;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_plus_menu, parent, false);
            holder = new ViewHolder();
            holder.tvIcon = convertView.findViewById(R.id.tv_icon);
            holder.tvText = convertView.findViewById(R.id.tv_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvIcon.setText(icons[position]);
        holder.tvText.setText(items[position]);
        return convertView;
    }

    private static class ViewHolder {
        TextView tvIcon;
        TextView tvText;
    }
}
