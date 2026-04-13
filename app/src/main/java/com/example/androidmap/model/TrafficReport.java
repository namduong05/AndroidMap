package com.example.androidmap.model;

public class TrafficReport {
    private String id;
    private String type; // "Kẹt xe", "Tai nạn", "Công trình"
    private String description;
    private double lat;
    private double lng;
    private long timestamp;
    private String userId;

    public TrafficReport() {} // Required for Firebase

    public TrafficReport(String type, String description, double lat, double lng, long timestamp, String userId) {
        this.type = type;
        this.description = description;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public long getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
}
