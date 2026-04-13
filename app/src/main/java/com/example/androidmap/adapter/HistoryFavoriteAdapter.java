package com.example.androidmap.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.androidmap.R;
import java.util.ArrayList;
import java.util.List;

public class HistoryFavoriteAdapter extends RecyclerView.Adapter<HistoryFavoriteAdapter.ViewHolder> {
    private List<Item> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Item item);
    }

    public static class Item {
        public String id; // ID từ Firestore hoặc Name từ Room
        public String name;
        public String address;
        public double lat;
        public double lng;

        public Item(String id, String name, String address, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvAddress.setText(item.address);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        ImageButton btnDelete;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
