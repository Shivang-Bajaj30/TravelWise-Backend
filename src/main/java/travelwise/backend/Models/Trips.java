package travelwise.backend.Models;

import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "testing")
public class Trips {

    @Id
    private String id;

    private String userId;

    private String location;

    private Integer travelers;

    @Field("start_date")
    private String startDate;

    @Field("end_date")
    private String endDate;

    private String preferences;

    // Separate nested fields instead of one large JSON string
    private List<Place> places;
    private List<Hotel> hotels;
    private List<String> transportation;
    private List<String> costs;
    private List<ItineraryDay> itinerary;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Nested Classes for better structure
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        private String name;
        private String time;
        private String details;
        private Coordinates coordinates;
        private String pricing;
        private String bestTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hotel {
        private String name;
        private String address;
        private Coordinates coordinates;
        private String price;
        private String rating;
        private List<String> amenities;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryDay {
        private Integer day;
        private List<String> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates {
        private Double lat;
        private Double lng;
    }
}