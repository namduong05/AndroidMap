package com.example.androidmap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidmap.model.AutocompleteResponse;
import com.example.androidmap.network.GoogleMapApiService;
import com.example.androidmap.network.RetrofitClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String AUTOCOMPLETE_API_KEY = "YOUR_API_KEY";

    private GoogleMap googleMap;
    private Marker selectedMarker;
    private LatLng selectedDestination;

    private TextInputEditText searchInput;
    private ImageButton clearButton;
    private RecyclerView resultsList;
    private SearchAdapter searchAdapter;
    private TextView mapPlaceholder;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private GoogleMapApiService apiService;

    private boolean hasMapsKey;
    private boolean apiKeyWarned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apiService = RetrofitClient.getClient().create(GoogleMapApiService.class);

        searchInput = findViewById(R.id.searchInput);
        clearButton = findViewById(R.id.btnClear);
        resultsList = findViewById(R.id.searchResults);
        mapPlaceholder = findViewById(R.id.mapPlaceholder);

        hasMapsKey = !getString(R.string.google_maps_key).equals("YOUR_API_KEY");

        searchAdapter = new SearchAdapter(this::onPlaceSelected);
        resultsList.setLayoutManager(new LinearLayoutManager(this));
        resultsList.setAdapter(searchAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null && hasMapsKey) {
            mapPlaceholder.setVisibility(View.GONE);
            mapFragment.getMapAsync(this);
        } else {
            mapPlaceholder.setVisibility(View.VISIBLE);
        }

        setupSearchBar();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        LatLng defaultCenter = new LatLng(21.0278, 105.8342);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 12f));
    }

    private void setupSearchBar() {
        clearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchAdapter.setItems(null);
            resultsList.setVisibility(View.GONE);
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                clearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                debounceRunnable = () -> callAutocomplete(query);
                debounceHandler.postDelayed(debounceRunnable, 400);
            }
        });
    }

    private void callAutocomplete(String query) {
        if (query.isEmpty()) {
            searchAdapter.setItems(null);
            resultsList.setVisibility(View.GONE);
            return;
        }

        if (AUTOCOMPLETE_API_KEY.equals("YOUR_API_KEY")) {
            if (!apiKeyWarned) {
                Toast.makeText(this, R.string.error_missing_key, Toast.LENGTH_SHORT).show();
                apiKeyWarned = true;
            }
            searchAdapter.setItems(null);
            resultsList.setVisibility(View.GONE);
            return;
        }

        double lat = 0.0;
        double lon = 0.0;
        if (googleMap != null) {
            LatLng center = googleMap.getCameraPosition().target;
            lat = center.latitude;
            lon = center.longitude;
        }

        apiService.autocomplete(AUTOCOMPLETE_API_KEY, query, lat, lon)
                .enqueue(new Callback<AutocompleteResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AutocompleteResponse> call,
                                           @NonNull Response<AutocompleteResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(MainActivity.this,
                                    R.string.error_autocomplete, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        List<AutocompleteResponse.Feature> features = response.body().getFeatures();
                        searchAdapter.setItems(features);
                        resultsList.setVisibility(features.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<AutocompleteResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(MainActivity.this,
                                R.string.error_network, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onPlaceSelected(AutocompleteResponse.Feature feature) {
        if (googleMap == null) {
            Toast.makeText(this, R.string.error_map_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = feature.getLatitude();
        double lon = feature.getLongitude();
        if (lat == 0.0 && lon == 0.0) {
            Toast.makeText(this, R.string.error_location, Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng location = new LatLng(lat, lon);
        selectedDestination = location;

        if (selectedMarker == null) {
            selectedMarker = googleMap.addMarker(new MarkerOptions().position(location));
        } else {
            selectedMarker.setPosition(location);
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));
        resultsList.setVisibility(View.GONE);
        getRoute();
    }

    private void getRoute() {
        if (selectedDestination == null) {
            return;
        }
        // TODO: Integrate routing API based on selectedDestination.
    }
}