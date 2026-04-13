package com.example.androidmap;

import android.app.Application;
import com.google.android.libraries.places.api.Places;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Khởi tạo Google Places SDK
        String apiKey = BuildConfig.MAPS_API_KEY;
        if (!apiKey.isEmpty()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
    }
}
