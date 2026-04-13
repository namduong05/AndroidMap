package com.example.androidmap.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    private List<Route> routes;

    @SerializedName("status")
    private String status;

    public List<Route> getRoutes() { return routes; }
    public String getStatus() { return status; }

    public static class Route {
        @SerializedName("legs")
        private List<Leg> legs;

        @SerializedName("overview_polyline")
        private OverviewPolyline overviewPolyline;

        public List<Leg> getLegs() { return legs; }
        public OverviewPolyline getOverviewPolyline() { return overviewPolyline; }
    }

    public static class Leg {
        @SerializedName("distance")
        private TextValue distance;

        @SerializedName("duration")
        private TextValue duration;

        public TextValue getDistance() { return distance; }
        public TextValue getDuration() { return duration; }
    }

    public static class TextValue {
        @SerializedName("text")
        private String text;

        @SerializedName("value")
        private int value;

        public String getText() { return text; }
        public int getValue() { return value; }
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        private String points;

        public String getPoints() { return points; }
    }
}
