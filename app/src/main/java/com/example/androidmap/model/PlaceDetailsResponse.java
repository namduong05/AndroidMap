package com.example.androidmap.model;

import com.google.gson.annotations.SerializedName;

public class PlaceDetailsResponse {
    @SerializedName("result")
    private Result result;

    @SerializedName("status")
    private String status;

    public Result getResult() { return result; }
    public String getStatus() { return status; }

    public static class Result {
        @SerializedName("geometry")
        private Geometry geometry;

        public Geometry getGeometry() { return geometry; }
    }

    public static class Geometry {
        @SerializedName("location")
        private Location location;

        public Location getLocation() { return location; }
    }

    public static class Location {
        @SerializedName("lat")
        private double lat;

        @SerializedName("lng")
        private double lng;

        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }
}
