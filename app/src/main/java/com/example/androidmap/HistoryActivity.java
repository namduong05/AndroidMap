package com.example.androidmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.androidmap.adapter.HistoryFavoriteAdapter;
import com.example.androidmap.databinding.ActivityHistoryBinding;
import com.example.androidmap.db.AppDatabase;
import com.example.androidmap.model.SearchHistory;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;
    private HistoryFavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử tìm kiếm");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HistoryFavoriteAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent();
            intent.putExtra("lat", item.lat);
            intent.putExtra("lng", item.lng);
            intent.putExtra("name", item.name);
            setResult(RESULT_OK, intent);
            finish();
        });

        adapter.setOnDeleteClickListener(item -> deleteHistory(item));

        loadHistory();
    }

    private void loadHistory() {
        new Thread(() -> {
            List<SearchHistory> historyList = AppDatabase.getInstance(this).searchHistoryDao().getAllHistory();
            List<HistoryFavoriteAdapter.Item> items = new ArrayList<>();
            for (SearchHistory h : historyList) {
                items.add(new HistoryFavoriteAdapter.Item(h.getPlaceName(), h.getPlaceName(), h.getAddress(), h.getLat(), h.getLng()));
            }
            runOnUiThread(() -> adapter.setItems(items));
        }).start();
    }

    private void deleteHistory(HistoryFavoriteAdapter.Item item) {
        new Thread(() -> {
            AppDatabase.getInstance(this).searchHistoryDao().deleteByName(item.name);
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                loadHistory();
            });
        }).start();
    }
}
