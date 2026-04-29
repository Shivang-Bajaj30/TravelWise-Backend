package travelwise.backend.service;

import travelwise.backend.Models.Trips;
import travelwise.backend.Models.Trips.*;
import travelwise.backend.Repo.TripRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TripService {

    @Autowired
    private TripRepo tripRepo;

    @Autowired
    private GeminiService geminiService;

    /**
     * Generate itinerary and save to MongoDB
     */
    public Trips createTrip(String location, Integer travelers, String startDate,
                            String endDate, String preferences, String budget) {

        // Call Gemini to generate itinerary
        Map<String, Object> itineraryMap = geminiService.generateItinerary(
                location, travelers, startDate, endDate, preferences, budget
        );

        // Convert map to Trips object
        Trips trip = convertMapToTrips(location, travelers, startDate, endDate,
                preferences, itineraryMap);

        // Save to MongoDB
        return tripRepo.save(trip);
    }

    /**
     * Get trip by ID
     */
    public Trips getTripById(String id) {
        return tripRepo.findById(id).orElse(null);
    }

    // ============ CONVERSION HELPERS ============

    private Trips convertMapToTrips(String location, Integer travelers, String startDate,
                                    String endDate, String preferences, Map<String, Object> data) {

        List<Place> places = extractPlaces(data.get("places"));
        List<Hotel> hotels = extractHotels(data.get("hotels"));
        List<String> transportation = extractStringList(data.get("transportation"));
        List<String> costs = extractStringList(data.get("costs"));
        List<ItineraryDay> itinerary = extractItinerary(data.get("itinerary"));

        LocalDateTime now = LocalDateTime.now();

        return Trips.builder()
                .location(location)
                .travelers(travelers)
                .startDate(startDate)
                .endDate(endDate)
                .preferences(preferences)
                .places(places)
                .hotels(hotels)
                .transportation(transportation)
                .costs(costs)
                .itinerary(itinerary)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Place> extractPlaces(Object placesData) {
        if (!(placesData instanceof List<?> list)) {
            return new ArrayList<>();
        }

        return list.stream()
                .filter(p -> p instanceof Map)
                .map(p -> {
                    Map<String, Object> placeMap = (Map<String, Object>) p;
                    Coordinates coords = extractCoordinates(placeMap.get("coordinates"));

                    return new Place(
                            getString(placeMap, "name"),
                            getString(placeMap, "time"),
                            getString(placeMap, "details"),
                            coords,
                            getString(placeMap, "pricing"),
                            getString(placeMap, "bestTime")
                    );
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Hotel> extractHotels(Object hotelsData) {
        if (!(hotelsData instanceof List<?> list)) {
            return new ArrayList<>();
        }

        return list.stream()
                .filter(h -> h instanceof Map)
                .map(h -> {
                    Map<String, Object> hotelMap = (Map<String, Object>) h;
                    Coordinates coords = extractCoordinates(hotelMap.get("coordinates"));
                    List<String> amenities = extractStringList(hotelMap.get("amenities"));

                    return new Hotel(
                            getString(hotelMap, "name"),
                            getString(hotelMap, "address"),
                            coords,
                            getString(hotelMap, "price"),
                            getString(hotelMap, "rating"),
                            amenities,
                            getString(hotelMap, "description")
                    );
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<ItineraryDay> extractItinerary(Object itineraryData) {
        if (!(itineraryData instanceof List<?> list)) {
            return new ArrayList<>();
        }

        return list.stream()
                .filter(i -> i instanceof Map)
                .map(i -> {
                    Map<String, Object> dayMap = (Map<String, Object>) i;
                    Integer day = getInteger(dayMap, "day");
                    List<String> activities = extractStringList(dayMap.get("activities"));

                    return new ItineraryDay(day, activities);
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Coordinates extractCoordinates(Object coordData) {
        if (!(coordData instanceof Map<?, ?>)) {
            return new Coordinates(0.0, 0.0);
        }

        Map<String, Object> coordMap = (Map<String, Object>) coordData;
        Double lat = getDouble(coordMap, "lat");
        Double lng = getDouble(coordMap, "lng");

        return new Coordinates(lat, lng);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Object data) {
        if (!(data instanceof List<?> list)) {
            return new ArrayList<>();
        }

        return list.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}