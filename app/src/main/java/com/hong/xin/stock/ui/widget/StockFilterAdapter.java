package com.hong.xin.stock.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hong.xin.stock.data.model.Stock;
import com.hong.xin.stock.data.repository.StockRepository;
import com.hong.xin.stock.util.DebugLogger;

import java.util.ArrayList;
import java.util.List;

public class StockFilterAdapter extends ArrayAdapter<Stock> {

    private static final String TAG = "StockFilterAdapter";
    private final StockRepository repository;
    private volatile StockFilter stockFilter;
    private String typeFilter;

    public StockFilterAdapter(Context context) {
        this(context, null);
    }

    public StockFilterAdapter(Context context, String typeFilter) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.repository = StockRepository.getInstance();
        this.typeFilter = typeFilter;
        DebugLogger.i(TAG, "StockFilterAdapter created, stock count in repo: " + repository.getStockCount() + ", typeFilter: " + typeFilter);
    }

    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }

    public Stock getStock(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        Stock stock = getItem(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        String label;
        if (stock.isEtf()) {
            label = "[ETF] " + stock.getName() + " (" + stock.getCode() + ")";
        } else {
            label = stock.getName() + " (" + stock.getCode() + ")";
        }
        textView.setText(label);
        return convertView;
    }

    @NonNull
    public CharSequence convertSelectionToString(@NonNull Object object) {
        if (object instanceof Stock) {
            return ((Stock) object).getName();
        }
        return object.toString();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (stockFilter == null) {
            stockFilter = new StockFilter();
        }
        return stockFilter;
    }

    private class StockFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() < 1) {
                results.values = new ArrayList<Stock>();
                results.count = 0;
                return results;
            }

            String keyword = constraint.toString();
            DebugLogger.d(TAG, "performFiltering: keyword='" + keyword + "' typeFilter='" + typeFilter + "'");

            List<Stock> found = repository.searchAll(keyword, typeFilter);
            if (found == null) found = new ArrayList<>();

            DebugLogger.d(TAG, "performFiltering: found " + found.size() + " results");

            results.values = new ArrayList<>(found);
            results.count = found.size();
            DebugLogger.d(TAG, "performFiltering: returning count=" + results.count);
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            DebugLogger.d(TAG, "publishResults: count=" + results.count);
            setNotifyOnChange(false);
            clear();
            if (results.values != null && results.count > 0) {
                List<Stock> items = (List<Stock>) results.values;
                addAll(items);
                DebugLogger.d(TAG, "publishResults: added " + items.size() + " items");
            }
            setNotifyOnChange(true);
            notifyDataSetChanged();
        }
    }
}
