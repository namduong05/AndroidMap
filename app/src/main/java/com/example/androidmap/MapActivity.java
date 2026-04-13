package com.example.androidmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidmap.adapter.SearchAdapter;
import com.example.androidmap.api.RetrofitClient;
import com.example.androidmap.databinding.ActivityMapBinding;
import com.example.androidmap.db.AppDatabase;
import com.example.androidmap.model.DirectionsResponse;
import com.example.androidmap.model.SearchHistory;
import com.example.androidmap.model.TrafficReport;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private SearchAdapter searchAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private ListenerRegistration trafficListener;
    private Call<DirectionsResponse> directionsCall;
    
    private Marker destinationMarker;
    private Polyline routePolyline;
    private LatLng currentLocation;
    private LatLng selectedLatLng;
    private String selectedPlaceName;
    private String selectedPlaceAddress;
    
    private final String apiKey = com.example.androidmap.BuildConfig.MAPS_API_KEY;

    private final ActivityResultLauncher<Intent> listLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    String name = result.getData().getStringExtra("name");
                    if (lat != 0 && lng != 0) {
                        onLocationSelected(new LatLng(lat, lng), name, "Đã chọn từ danh sách");
                        calculateRoute(new LatLng(lat, lng));
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Cần đảm bảo SDK được khởi tạo đúng
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        setupSearch();
        setupUI();
        setupSidebar();
    }

    private void setupSidebar() {
        View headerView = binding.navView.getHeaderView(0);
        TextView tvUserEmail = headerView.findViewById(R.id.tv_user_email);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            tvUserEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites) {
                listLauncher.launch(new Intent(this, FavoritesActivity.class));
            } else if (id == R.id.nav_history) {
                listLauncher.launch(new Intent(this, HistoryActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupSearch() {
        searchAdapter = new SearchAdapter();
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(searchAdapter);
        binding.rvSearchResults.setZ(10f); // Đảm bảo nổi trên bản đồ

        searchAdapter.setOnItemClickListener(prediction -> {
            hideKeyboard();
            binding.etSearch.setText(prediction.getPrimaryText(null).toString());
            binding.rvSearchResults.setVisibility(View.GONE);
            fetchPlaceDetailsFromSDK(prediction.getPlaceId());
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    if (s.length() > 1) searchPlacesSDK(s.toString());
                    else binding.rvSearchResults.setVisibility(View.GONE);
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchPlacesSDK(String query) {
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setCountries("VN")
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
            searchAdapter.setPredictionsSDK(predictions);
            binding.rvSearchResults.setVisibility(predictions.isEmpty() ? View.GONE : View.VISIBLE);
        }).addOnFailureListener(e -> {
            if (e instanceof ApiException) {
                ApiException apiException = (ApiException) e;
                Log.e("PLACES_ERROR", "Error: " + apiException.getStatusCode());
                // Nếu bị từ chối do API Key: REQUEST_DENIED (2500)
            }
        });
    }

    private void fetchPlaceDetailsFromSDK(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            if (place.getLatLng() != null) {
                String name = place.getName() != null ? place.getName() : "Địa điểm đã chọn";
                new Thread(() -> AppDatabase.getInstance(this).searchHistoryDao().insert(
                        new SearchHistory(name, place.getAddress(), place.getLatLng().latitude, place.getLatLng().longitude, System.currentTimeMillis()))).start();
                onLocationSelected(place.getLatLng(), name, place.getAddress());
                // Tự động tính đường khi chọn kết quả tìm kiếm
                calculateRoute(place.getLatLng());
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi lấy chi tiết địa điểm", Toast.LENGTH_SHORT).show());
    }

    private void setupUI() {
        binding.fabMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnFavorite.setOnClickListener(v -> saveToFavorites());
        binding.btnCancel.setOnClickListener(v -> clearRoute());
        binding.btnGetDirections.setOnClickListener(v -> { if (selectedLatLng != null) calculateRoute(selectedLatLng); });
        binding.rgVehicleMode.setOnCheckedChangeListener((group, id) -> { if (selectedLatLng != null) calculateRoute(selectedLatLng); });
    }

    private void onLocationSelected(LatLng latLng, String name, String address) {
        selectedLatLng = latLng;
        selectedPlaceName = name;
        selectedPlaceAddress = address;

        if (destinationMarker != null) destinationMarker.remove();
        if (routePolyline != null) routePolyline.remove();

        destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(name));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        
        binding.tvSelectedPlaceName.setText(name);
        binding.tvSelectedPlaceAddress.setText(address != null ? address : "Điểm đã chọn");
        binding.bottomPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this::getAddressFromLatLng);
        updateLocationUI();
        refreshCurrentLocation(true);

        trafficListener = FirebaseFirestore.getInstance().collection("traffic_reports")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                TrafficReport report = dc.getDocument().toObject(TrafficReport.class);
                                mMap.addMarker(new MarkerOptions().position(new LatLng(report.getLat(), report.getLng()))
                                        .title(report.getType()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                            }
                        }
                    }
                });
    }

    private void getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> adrs = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (adrs != null && !adrs.isEmpty()) onLocationSelected(latLng, adrs.get(0).getFeatureName(), adrs.get(0).getAddressLine(0));
            else onLocationSelected(latLng, "Điểm đã chọn", "");
        } catch (IOException e) { onLocationSelected(latLng, "Điểm đã chọn", ""); }
    }

    private void updateLocationUI() {
        if (mMap == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setPadding(0, 150, 0, 0); 
    }

    private void refreshCurrentLocation(boolean moveCamera) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (moveCamera) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
            }
        });
    }

    private void moveToCurrentLocation() {
        if (currentLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
            refreshCurrentLocation(false); 
        } else refreshCurrentLocation(true);
    }

    private void calculateRoute(LatLng destination) {
        if (currentLocation == null) {
            Toast.makeText(this, "Đang xác định vị trí của bạn...", Toast.LENGTH_SHORT).show();
            refreshCurrentLocation(false);
            return;
        }

        if (directionsCall != null) directionsCall.cancel();
        binding.btnGetDirections.setEnabled(false);
        binding.tvDuration.setText("Đang tìm...");

        String mode = binding.rgVehicleMode.getCheckedRadioButtonId() == R.id.rb_walking ? "walking" : "driving";
        String origin = currentLocation.latitude + "," + currentLocation.longitude;
        String dest = destination.latitude + "," + destination.longitude;

        Log.d("ROUTE_DEBUG", "Origin: " + origin + " | Dest: " + dest + " | Key: " + apiKey);

        directionsCall = RetrofitClient.getApiService().getDirections(origin, dest, mode, "vi", apiKey);
        directionsCall.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                binding.btnGetDirections.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getStatus();
                    if ("OK".equals(status) && !response.body().getRoutes().isEmpty()) {
                        drawRoute(response.body().getRoutes().get(0));
                    } else {
                        Log.e("ROUTE_ERROR", "Status: " + status);
                        handleDirectionsError(status);
                    }
                } else {
                    Log.e("ROUTE_ERROR", "Response Code: " + response.code());
                    Toast.makeText(MapActivity.this, "Lỗi API Chỉ đường", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    binding.btnGetDirections.setEnabled(true);
                    Log.e("ROUTE_ERROR", "Failed: " + t.getMessage());
                    Toast.makeText(MapActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleDirectionsError(String status) {
        String msg = "Không thể tìm đường";
        if ("ZERO_RESULTS".equals(status)) msg = "Không tìm thấy đường đi";
        else if ("OVER_QUERY_LIMIT".equals(status)) msg = "Hết hạn ngạch API (Cần bật Billing)";
        else if ("REQUEST_DENIED".equals(status)) msg = "API bị từ chối (Kiểm tra API Key)";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        binding.tvDuration.setText("Lỗi: " + status);
    }

    private void drawRoute(DirectionsResponse.Route route) {
        if (routePolyline != null) routePolyline.remove();
        List<LatLng> points = PolyUtil.decode(route.getOverviewPolyline().getPoints());
        routePolyline = mMap.addPolyline(new PolylineOptions().addAll(points).color(Color.parseColor("#4285F4")).width(15).geodesic(true));
        
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng p : points) builder.include(p);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        
        DirectionsResponse.Leg leg = route.getLegs().get(0);
        binding.tvDistance.setText("K.cách: " + leg.getDistance().getText());
        binding.tvDuration.setText("Thời gian: " + leg.getDuration().getText());
    }

    private void clearRoute() {
        if (destinationMarker != null) destinationMarker.remove();
        if (routePolyline != null) routePolyline.remove();
        binding.bottomPanel.setVisibility(View.GONE);
        binding.etSearch.setText("");
        selectedLatLng = null;
    }

    private void saveToFavorites() {
        if (selectedLatLng == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        Map<String, Object> fav = new HashMap<>();
        fav.put("name", selectedPlaceName); fav.put("address", selectedPlaceAddress);
        fav.put("lat", selectedLatLng.latitude); fav.put("lng", selectedLatLng.longitude);
        fav.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("favorites").add(fav).addOnSuccessListener(d -> Toast.makeText(this, "Đã lưu yêu thích", Toast.LENGTH_SHORT).show());
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
        if (trafficListener != null) trafficListener.remove();
        if (directionsCall != null) directionsCall.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocationUI(); refreshCurrentLocation(true);
        }
    }
}
