package travelwise.backend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "packages")
public class Packages {

    @Id
    private String id;
    private String name;
    private String description;
    private String image;
    private String price;
    private String duration;
    private List<String> destinations;
    private List<String> includes;
    private double rating;
    private int reviews;
    private int people;

    private ItineraryData itineraryData;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryData {
        private List<Place> places;
        private List<Hotel> hotels;
        private List<String> transportation;
        private List<String> costs;
        private List<DailyItinerary> itinerary;
        private String budget;
        private String duration;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        private String name;
        private String details;
        private String time;
        private String pricing;
        private String bestTime;
        private String location;
        private Coords coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hotel {
        private String name;
        private String address;
        private String price;
        private double rating;
        private List<String> amenities;
        private String description;
        private String location;
        private Coords coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyItinerary {
        private int day;
        private List<String> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coords {
        private double lat;
        private double lng;
    }
}