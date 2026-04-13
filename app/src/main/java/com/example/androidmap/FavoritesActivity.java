package com.example.androidmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.androidmap.adapter.HistoryFavoriteAdapter;
import com.example.androidmap.databinding.ActivityFavoritesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private ActivityFavoritesBinding binding;
    private HistoryFavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Địa điểm yêu thích");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HistoryFavoriteAdapter();
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent();
            intent.putExtra("lat", item.lat);
            intent.putExtra("lng", item.lng);
            intent.putExtra("name", item.name);
            setResult(RESULT_OK, intent);
            finish();
        });

        adapter.setOnDeleteClickListener(item -> deleteFavorite(item));

        loadFavorites();
    }

    private void loadFavorites() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("favorites")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HistoryFavoriteAdapter.Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        items.add(new HistoryFavoriteAdapter.Item(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("address"),
                                lat != null ? lat : 0.0,
                                lng != null ? lng : 0.0
                        ));
                    }
                    adapter.setItems(items);
                });
    }

    private void deleteFavorite(HistoryFavoriteAdapter.Item item) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("favorites").document(item.id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    loadFavorites();
                });
    }
}
