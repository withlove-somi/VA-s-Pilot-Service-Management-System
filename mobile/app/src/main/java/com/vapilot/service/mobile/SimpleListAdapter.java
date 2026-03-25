package com.vapilot.service.mobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {
    private final List<String> items = new ArrayList<>();

    public void submitList(List<String> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.item_text);
        }

        void bind(String value) {
            text.setText(value);
        }
    }
}
