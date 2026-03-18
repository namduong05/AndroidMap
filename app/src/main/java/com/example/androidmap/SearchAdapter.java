package com.example.androidmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidmap.model.AutocompleteResponse;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    public interface OnPlaceClickListener {
        void onPlaceClick(AutocompleteResponse.Feature feature);
    }

    private final List<AutocompleteResponse.Feature> items = new ArrayList<>();
    private final OnPlaceClickListener listener;

    public SearchAdapter(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AutocompleteResponse.Feature> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        AutocompleteResponse.Feature feature = items.get(position);
        AutocompleteResponse.Properties properties = feature.getProperties();
        String name = properties != null ? properties.getDisplayName() : "";
        String address = properties != null ? properties.getDisplayAddress() : "";
        holder.nameText.setText(name);
        holder.addressText.setText(address);
        holder.itemView.setOnClickListener(v -> listener.onPlaceClick(feature));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView addressText;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.resultName);
            addressText = itemView.findViewById(R.id.resultAddress);
        }
    }
}
