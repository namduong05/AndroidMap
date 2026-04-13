package com.example.androidmap.api;

import com.example.androidmap.model.DirectionsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleApiService {
    // Chúng ta dùng Places SDK cho Tìm kiếm, nên chỉ cần Directions ở đây
    @GET("directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("language") String language,
            @Query("key") String apiKey
    );
}
