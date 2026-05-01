package travelwise.backend.controller;

import travelwise.backend.Models.Trips;
import travelwise.backend.security.JwtUtil;
import travelwise.backend.service.TripService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@Slf4j
@CrossOrigin(origins = "*")

public class TripsController {

    @Autowired
    private TripService tripsService;

    @Autowired
    private JwtUtil jwtUtil;

    /** * Extract userId from the Authorization header (Bearer token) */
    private String getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        var claims = jwtUtil.parseToken(token);
        return claims.get("id", String.class);
    }

    /** * Create a new trip with AI-generated itinerary (linked to logged-in user) */
    @PostMapping("/create")
    public ResponseEntity<Trips> createTrip(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String location,
            @RequestParam Integer travelers,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "") String preferences,
            @RequestParam(defaultValue = "moderate") String budget) {
        try {
            String userId = getUserIdFromHeader(authHeader);
            Trips trip = tripsService.createTrip(userId, location, travelers, startDate, endDate,
                    preferences, budget);
            return ResponseEntity.status(HttpStatus.CREATED).body(trip);
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all trips for the logged-in user
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserTrips(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }
            List<Trips> trips = tripsService.getTripsByUserId(userId);
            return ResponseEntity.ok(trips);
        } catch (Exception e) {
            log.error("Error fetching user trips: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}