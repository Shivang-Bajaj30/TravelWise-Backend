package com.travelwise.Backend.Controller;

import com.travelwise.Backend.Model.User;
import com.travelwise.Backend.Service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${olamaps.api.key}")
    private String olaMapsApiKey;

    @PostMapping("auth/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        try {
            userService.registerUser(user);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        try {
            User user = userService.login(email, password);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if ("User not found".equals(message)) {
                return ResponseEntity.status(404).body(message);
            } else if ("Incorrect password".equals(message)) {
                return ResponseEntity.status(401).body(message);
            } else {
                return ResponseEntity.badRequest().body("Login failed");
            }
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<List<OlaMapsResult>> getLocationSuggestions(@RequestParam String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.olamaps.io/places/v1/autocomplete")
                    .queryParam("input", query)
                    .queryParam("api_key", olaMapsApiKey)
                    .queryParam("limit", 5)
                    .toUriString();
            System.out.println("Request URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Request-Id", UUID.randomUUID().toString());
            headers.set("X-Correlation-Id", UUID.randomUUID().toString());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getBody() == null || !response.getBody().containsKey("predictions")) {
                return ResponseEntity.noContent().build();
            }

            List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.getBody().get("predictions");

            List<OlaMapsResult> results = new ArrayList<>();
            for (Map<String, Object> prediction : predictions) {
                String description = (String) prediction.get("description");
                Map<String, Object> geometry = (Map<String, Object>) prediction.get("geometry");
                if (description != null && geometry != null) {
                    Map<String, Double> location = (Map<String, Double>) geometry.get("location");
                    if (location != null) {
                        OlaMapsResult result = new OlaMapsResult();
                        result.setDescription(description);
                        result.setGeometry(new Geometry(location.get("lat"), location.get("lng")));
                        result.setPlace_id((String) prediction.get("place_id"));
                        results.add(result);
                    }
                }
            }

            return ResponseEntity.ok(results);

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/trip")
    public ResponseEntity<String> submitTripDetails(@RequestBody TripDetails tripDetails) {
        try {
            System.out.println("Received trip details: " + tripDetails);
            return ResponseEntity.ok("Trip details received successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process trip details: " + e.getMessage());
        }
    }

    @Getter
    @Setter
    static class OlaMapsResult {
        private String description;
        private Geometry geometry;
        private String place_id;
    }

    @Getter
    @Setter
    static class Geometry {
        private Location location;

        public Geometry(Double lat, Double lng) {
            this.location = new Location(lat, lng);
        }
    }

    @Getter
    @Setter
    static class Location {
        private Double lat;
        private Double lng;

        public Location(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    @Getter
    @Setter
    static class TripDetails {
        private String location;
        private int travelers;
        private String startDate;
        private String endDate;
        private String preferences;

        @Override
        public String toString() {
            return "TripDetails{" +
                    "location='" + location + '\'' +
                    ", travelers=" + travelers +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", preferences='" + preferences + '\'' +
                    '}';
        }
    }
}