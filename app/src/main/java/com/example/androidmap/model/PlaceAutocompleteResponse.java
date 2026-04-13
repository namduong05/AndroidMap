package com.example.androidmap.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PlaceAutocompleteResponse {
    @SerializedName("predictions")
    private List<Prediction> predictions;

    @SerializedName("status")
    private String status;

    public List<Prediction> getPredictions() { return predictions; }
    public String getStatus() { return status; }

    public static class Prediction {
        @SerializedName("description")
        private String description;

        @SerializedName("place_id")
        private String placeId;

        @SerializedName("structured_formatting")
        private StructuredFormatting structuredFormatting;

        public String getDescription() { return description; }
        public String getPlaceId() { return placeId; }
        public StructuredFormatting getStructuredFormatting() { return structuredFormatting; }
    }

    public static class StructuredFormatting {
        @SerializedName("main_text")
        private String mainText;

        @SerializedName("secondary_text")
        private String secondaryText;

        public String getMainText() { return mainText; }
        public String getSecondaryText() { return secondaryText; }
    }
}
