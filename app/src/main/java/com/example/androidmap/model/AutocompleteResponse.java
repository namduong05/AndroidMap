package com.example.androidmap.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class AutocompleteResponse {
    @SerializedName("type")
    private String type;

    @SerializedName("features")
    private List<Feature> features;

    public String getType() {
        return type;
    }

    public List<Feature> getFeatures() {
        if (features == null) {
            return Collections.emptyList();
        }
        return features;
    }

    public static class Feature {
        @SerializedName("type")
        private String type;

        @SerializedName("properties")
        private Properties properties;

        @SerializedName("geometry")
        private Geometry geometry;

        public String getType() {
            return type;
        }

        public Properties getProperties() {
            return properties;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public double getLatitude() {
            if (geometry == null || geometry.coordinates == null || geometry.coordinates.size() < 2) {
                return 0.0;
            }
            return geometry.coordinates.get(1);
        }

        public double getLongitude() {
            if (geometry == null || geometry.coordinates == null || geometry.coordinates.size() < 2) {
                return 0.0;
            }
            return geometry.coordinates.get(0);
        }
    }

    public static class Properties {
        @SerializedName("name")
        private String name;

        @SerializedName("address")
        private String address;

        @SerializedName("formatted_address")
        private String formattedAddress;

        @SerializedName("label")
        private String label;

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public String getFormattedAddress() {
            return formattedAddress;
        }

        public String getLabel() {
            return label;
        }

        public String getDisplayName() {
            if (name != null && !name.isEmpty()) {
                return name;
            }
            if (label != null && !label.isEmpty()) {
                return label;
            }
            return "";
        }

        public String getDisplayAddress() {
            if (address != null && !address.isEmpty()) {
                return address;
            }
            if (formattedAddress != null && !formattedAddress.isEmpty()) {
                return formattedAddress;
            }
            return "";
        }
    }

    public static class Geometry {
        @SerializedName("type")
        private String type;

        @SerializedName("coordinates")
        private List<Double> coordinates;

        public String getType() {
            return type;
        }

        public List<Double> getCoordinates() {
            return coordinates;
        }
    }
}
