package travelwise.backend.controller;

import travelwise.backend.Models.Trips;
import travelwise.backend.service.TripService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@Slf4j
@CrossOrigin(origins = "*")

public class TripsController {

    @Autowired
    private TripService tripsService;

    /**
     * Create a new trip with AI-generated itinerary
     */
    @PostMapping("/create")
    public ResponseEntity<Trips> createTrip(
            @RequestParam String location,
            @RequestParam Integer travelers,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "") String preferences,
            @RequestParam(defaultValue = "moderate") String budget) {

        try {
            Trips trip = tripsService.createTrip(location, travelers, startDate, endDate,
                    preferences, budget);
            return ResponseEntity.status(HttpStatus.CREATED).body(trip);
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get trip by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Trips> getTrip(@PathVariable String id) {
        try {
            Trips trip = tripsService.getTripById(id);
            if (trip != null) {
                return ResponseEntity.ok(trip);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving trip: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}