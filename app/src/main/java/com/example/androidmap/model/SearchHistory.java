package com.example.androidmap.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistory {
    @PrimaryKey
    @NonNull
    private String placeName;
    private String address;
    private double lat;
    private double lng;
    private long timestamp;

    public SearchHistory(@NonNull String placeName, String address, double lat, double lng, long timestamp) {
        this.placeName = placeName;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getPlaceName() { return placeName; }
    public void setPlaceName(@NonNull String placeName) { this.placeName = placeName; }
    public String getAddress() { return address; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
