//package com.travelwise.Backend.Controller;
//
//import com.travelwise.Backend.Model.User;
//import com.travelwise.Backend.Service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    @PostMapping("/signup")
//    public ResponseEntity<String> signup(@RequestBody User user) {
//        try {
//            userService.registerUser(user);
//            return ResponseEntity.ok("User registered successfully");
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
//        String email = credentials.get("email");
//        String password = credentials.get("password");
//
//        try {
//            User user = userService.login(email, password);
//            return ResponseEntity.ok(user);
//        } catch (RuntimeException e) {
//            String message = e.getMessage();
//            if ("User not found".equals(message)) {
//                return ResponseEntity.status(404).body(message);
//            } else if ("Incorrect password".equals(message)) {
//                return ResponseEntity.status(401).body(message);
//            } else {
//                return ResponseEntity.badRequest().body("Login failed");
//            }
//        }
//    }
//}



package com.travelwise.Backend.Controller;

import com.travelwise.Backend.Model.User;
import com.travelwise.Backend.Service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @Value("${mapbox.api.key}")
    private String mapboxApiKey;

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

    // ✅ Mapbox-based location suggestions
    @GetMapping("/locations")
    public ResponseEntity<List<NominatimResult>> getLocationSuggestions(@RequestParam String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.mapbox.com/geocoding/v5/mapbox.places/{query}.json")
                    .queryParam("access_token", mapboxApiKey)
                    .queryParam("autocomplete", "true")
                    .queryParam("limit", 5)
                    .buildAndExpand(query)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("features")) {
                return ResponseEntity.noContent().build();
            }

            List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");

            List<NominatimResult> results = new ArrayList<>();
            for (Map<String, Object> feature : features) {
                String placeName = (String) feature.get("place_name");
                if (placeName != null) {
                    NominatimResult result = new NominatimResult();
                    result.setDisplay_name(placeName);
                    results.add(result);
                }
            }

            return ResponseEntity.ok(results);

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
    static class NominatimResult {
        private String display_name;
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
