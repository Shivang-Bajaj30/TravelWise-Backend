package travelwise.backend.controller;

import tools.jackson.databind.ObjectMapper;
import travelwise.backend.dto.ItineraryRequest;
import travelwise.backend.Models.Trips;
import travelwise.backend.Repo.TripRepo;
import travelwise.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ItineraryController {

    private final GeminiService geminiService;
    private final TripRepo tripRepo;
    private final ObjectMapper objectMapper;

    @PostMapping("/generate_itinerary")
    public ResponseEntity<?> generateItinerary(@RequestBody ItineraryRequest req) {
        try {
            String destination = req.getEffectiveDestination();
            if (destination == null || req.getTravelers() == null ||
                req.getStartDate() == null || req.getEndDate() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "All required fields must be filled"));
            }

            Map<String, Object> aiResponse = geminiService.generateItinerary(
                    destination, req.getTravelers(), req.getStartDate(),
                    req.getEndDate(), req.getPreferences(),
                    req.getBudget(), req.getTravelWith()
            );

            // Save to DB
            Trips trip = Trips.builder()
                    .location(destination)
                    .travelers(req.getTravelers())
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .preferences(req.getPreferences())
                    .itineraryData(objectMapper.writeValueAsString(aiResponse))
                    .build();
            tripRepo.save(trip);

            return ResponseEntity.ok(Map.of(
                    "message", "Itinerary generated successfully!",
                    "data", aiResponse
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate itinerary",
                    "details", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }
}
