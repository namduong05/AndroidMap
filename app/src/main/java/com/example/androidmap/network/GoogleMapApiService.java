package com.example.androidmap.network;

import com.example.androidmap.model.AutocompleteResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleMapApiService {
    @GET("autocomplete/v4")
    Call<AutocompleteResponse> autocomplete(
            @Query("apikey") String apiKey,
            @Query("text") String text,
            @Query("lat") double lat,
            @Query("lon") double lon
    );
}
