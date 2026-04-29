package travelwise.backend.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.*;

@Service
@Slf4j
public class GeminiService {

    // Primary Provider Configuration
    @Value("${ai.primary-provider:gemini}")
    private String primaryProvider;

    @Value("${ai.fallback-provider:groq}")
    private String fallbackProvider;

    // Gemini Configuration
    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    private static final String[] GEMINI_MODELS = {
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };

    // Groq Configuration - Multiple models to try
    @Value("${groq.api-key:}")
    private String groqApiKey;

    private static final String[] GROQ_MODELS = {
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "openai/gpt-oss-20b"
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // ---- Date parsing ----
    private LocalDate parseDate(String d) {
        String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd/MM/yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(d, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        return LocalDate.parse(d); // ISO fallback
    }

    // ---- Call AI API (route to Gemini or Groq) ----
    @SuppressWarnings("unchecked")
    private String callAI(String prompt, String provider) {
        if ("groq".equalsIgnoreCase(provider)) {
            return callGroq(prompt);
        } else {
            return callGemini(prompt);
        }
    }

    // ---- Call Gemini REST API (with model fallback) ----
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        Exception lastError = null;

        for (String model : GEMINI_MODELS) {
            try {
                log.info("[GeminiService] Trying Gemini model: {}", model);
                String url = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                        model, geminiApiKey
                );

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(Map.of(
                                "parts", List.of(Map.of("text", prompt))
                        )),
                        "generationConfig", Map.of(
                                "temperature", 0.2,
                                "maxOutputTokens", 8000,
                                "responseMimeType", "application/json"
                        ),
                        "systemInstruction", Map.of(
                                "parts", List.of(Map.of("text",
                                        "You are a travel itinerary assistant that returns only valid JSON."))
                        )
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                // Extract text from Gemini response structure
                var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                var content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (List<Map<String, Object>>) content.get("parts");
                String text = (String) parts.get(0).get("text");

                log.info("[GeminiService] ✅ Gemini succeeded");
                return text.trim();

            } catch (Exception e) {
                lastError = e;
                String errMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                boolean isRateLimit = errMsg.contains("429") || errMsg.contains("quota")
                        || errMsg.contains("resource_exhausted") || errMsg.contains("rate");

                if (isRateLimit) {
                    log.warn("[GeminiService] ⚠️ {} rate limited, trying next model...", model);
                    continue;
                }
                throw new RuntimeException("Gemini API error: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("All Gemini models exhausted (rate limited).", lastError);
    }

    // ---- Call Groq/Llama API (with model fallback) ----
    @SuppressWarnings("unchecked")
    private String callGroq(String prompt) {
        Exception lastError = null;

        for (String model : GROQ_MODELS) {
            try {
                log.info("[GeminiService] Trying Groq model: {}", model);

                String url = "https://api.groq.com/openai/v1/chat/completions";

                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content",
                                        "You are a travel itinerary assistant that returns only valid JSON."),
                                Map.of("role", "user", "content", prompt)
                        ),
                        "temperature", 0.2,
                        "max_tokens", 8000
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + groqApiKey);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                var choices = (List<Map<String, Object>>) response.getBody().get("choices");
                var message = (Map<String, Object>) choices.get(0).get("message");
                String text = (String) message.get("content");

                log.info("[GeminiService] ✅ Groq succeeded with model: {}", model);
                return text.trim();

            } catch (Exception e) {
                lastError = e;
                String errMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                // Check if model is decommissioned or unavailable
                boolean isModelIssue = errMsg.contains("decommissioned") ||
                        errMsg.contains("not found") ||
                        errMsg.contains("invalid_request_error");

                if (isModelIssue) {
                    log.warn("[GeminiService] ⚠️ {} unavailable, trying next Groq model...", model);
                    continue;
                }

                // If it's a rate limit, also try next model
                boolean isRateLimit = errMsg.contains("429") || errMsg.contains("quota") ||
                        errMsg.contains("rate_limit");
                if (isRateLimit) {
                    log.warn("[GeminiService] ⚠️ {} rate limited, trying next model...", model);
                    continue;
                }

                throw e;
            }
        }
        throw new RuntimeException("All Groq models exhausted or unavailable.", lastError);
    }

    // ---- JSON extraction ----
    private String extractJson(String content) {
        // Remove control characters except newlines/tabs
        content = content.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

        // Remove Markdown code fences if present
        Pattern fencePattern = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
        Matcher m = fencePattern.matcher(content);
        if (m.find()) {
            content = m.group(1).trim();
        }

        // Find outermost JSON object if not starting with {
        if (!content.startsWith("{")) {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}") + 1;
            if (start != -1 && end > start) {
                content = content.substring(start, end);
            }
        }

        // Repair truncated JSON (missing closing brackets/braces)
        long openBraces = content.chars().filter(c -> c == '{').count()
                - content.chars().filter(c -> c == '}').count();
        long openBrackets = content.chars().filter(c -> c == '[').count()
                - content.chars().filter(c -> c == ']').count();

        if (openBraces > 0 || openBrackets > 0) {
            content += "]".repeat((int) Math.max(0, openBrackets));
            content += "}".repeat((int) Math.max(0, openBraces));
        }

        return content.trim();
    }

    // ---- Generate itinerary chunk ----
    private Map<String, Object> generateItineraryChunk(
            String destination, int travelers, String startDate,
            String endDate, String preferences, String budget) {

        int daysCount;
        try {
            LocalDate sd = parseDate(startDate);
            LocalDate ed = parseDate(endDate);
            daysCount = (int) ChronoUnit.DAYS.between(sd, ed) + 1;
            if (daysCount <= 0) daysCount = 1;
        } catch (Exception e) {
            daysCount = 1;
        }
        String budgetDisplay = (budget != null && !budget.isEmpty()) ? budget : "unspecified";

        String prompt = """
            Create a %d-day travel itinerary for %d people visiting %s on a %s budget.
            Use real, famous places and hotels that actually exist in %s.
            Include 4-5 hotels with varying price ranges. Keep descriptions short (1 sentence max).
            Do NOT include any Google Maps URLs. Only include coordinates.

            Return a valid JSON object with this exact structure:

            {
                "places": [
                    {
                        "name": "Real place name",
                        "time": "2-3 hours",
                        "details": "One sentence description",
                        "coordinates": {"lat": 0.0, "lng": 0.0},
                        "pricing": "Entry fee",
                        "bestTime": "Best time to visit"
                    }
                ],
                "hotels": [
                    {
                        "name": "Real hotel name",
                        "address": "Short address",
                        "coordinates": {"lat": 0.0, "lng": 0.0},
                        "price": "Price range per night",
                        "rating": "4.5/5",
                        "amenities": ["WiFi", "Pool"],
                        "description": "One sentence"
                    }
                ],
                "transportation": ["Option 1", "Option 2"],
                "costs": ["Accommodation: ₹X", "Food: ₹Y"],
                "itinerary": [{"day": 1, "activities": ["Morning: Activity", "Afternoon: Activity"]}]
            }

            IMPORTANT: The 'itinerary' array MUST contain EXACTLY %d objects. Number 'day' fields sequentially from 1 to %d.
            """.formatted(daysCount, travelers, destination, budgetDisplay, destination, daysCount, daysCount);

        try {
            // Try primary provider
            log.info("[GeminiService] Using primary provider: {}", primaryProvider);
            String content = callAI(prompt, primaryProvider);
            content = extractJson(content);

            Map<String, Object> itineraryData;
            try {
                itineraryData = objectMapper.readValue(content, new TypeReference<>() {});
            } catch (Exception e) {
                log.error("[GeminiService] Invalid JSON from primary provider");
                throw e;
            }

            // Validate days count and retry once if needed
            if (itineraryData.containsKey("itinerary")) {
                var itinerary = (List<?>) itineraryData.get("itinerary");
                if (itinerary != null && itinerary.size() != daysCount) {
                    log.warn("[GeminiService] Model returned {} days but {} requested. Retrying...",
                            itinerary.size(), daysCount);

                    String retryPrompt = prompt + "\nYou returned " + itinerary.size()
                            + " days but required " + daysCount
                            + ". Return EXACTLY " + daysCount + " days now, keep same style.\n";

                    String content2 = callAI(retryPrompt, primaryProvider);
                    content2 = extractJson(content2);

                    try {
                        Map<String, Object> retryData = objectMapper.readValue(content2, new TypeReference<>() {});
                        itineraryData = retryData;
                    } catch (Exception ex) {
                        log.error("[GeminiService] Retry also returned invalid JSON.");
                    }
                }
            }

            return itineraryData;

        } catch (Exception e) {
            log.warn("[GeminiService] Primary provider failed, trying fallback: {}", fallbackProvider);

            // Try fallback provider
            try {
                String content = callAI(prompt, fallbackProvider);
                content = extractJson(content);
                Map<String, Object> itineraryData = objectMapper.readValue(content, new TypeReference<>() {});
                log.info("[GeminiService] ✅ Fallback provider succeeded");
                return itineraryData;
            } catch (Exception e2) {
                log.error("[GeminiService] Both providers failed: {}", e2.getMessage());

                // Return fallback itinerary
                List<Map<String, Object>> fallbackItinerary = new ArrayList<>();
                for (int i = 0; i < daysCount; i++) {
                    fallbackItinerary.add(Map.of(
                            "day", i + 1,
                            "activities", List.of("Explore local attractions", "Try local cuisine")
                    ));
                }

                Map<String, Object> fallback = new HashMap<>();
                fallback.put("places", Collections.emptyList());
                fallback.put("hotels", Collections.emptyList());
                fallback.put("transportation", List.of("Public Transport"));
                fallback.put("costs", List.of("Budget varies"));
                fallback.put("itinerary", fallbackItinerary);
                fallback.put("error", "Both providers failed");
                fallback.put("source", "error");
                return fallback;
            }
        }
    }

    // ---- Main public method: splits into 7-day chunks if needed ----
    public Map<String, Object> generateItinerary(
            String destination, int travelers, String startDate,
            String endDate, String preferences, String budget) {

        LocalDate sd, ed;
        int daysCount;
        try {
            sd = parseDate(startDate);
            ed = parseDate(endDate);
            daysCount = (int) ChronoUnit.DAYS.between(sd, ed) + 1;
            if (daysCount <= 0) daysCount = 1;
        } catch (Exception e) {
            // Cannot parse dates, fall back to single chunk
            return generateItineraryChunk(destination, travelers, startDate, endDate,
                    preferences, budget);
        }

        if (daysCount > 7) {
            // Split into 7-day chunks (same logic as Python)
            List<Map<String, Object>> allItinerary = new ArrayList<>();
            List<Object> allPlaces = new ArrayList<>();
            List<Object> allHotels = new ArrayList<>();
            Set<String> allTransportation = new LinkedHashSet<>();
            List<Object> allCosts = new ArrayList<>();
            int dayNum = 1;

            LocalDate chunkStart = sd;
            while (!chunkStart.isAfter(ed)) {
                LocalDate chunkEnd = chunkStart.plusDays(6);
                if (chunkEnd.isAfter(ed)) chunkEnd = ed;

                Map<String, Object> chunkResult = generateItineraryChunk(
                        destination, travelers,
                        chunkStart.toString(), chunkEnd.toString(),
                        preferences, budget
                );

                // Merge itinerary days with correct numbering
                if (chunkResult.get("itinerary") instanceof List<?> itin) {
                    for (Object dayObj : itin) {
                        if (dayObj instanceof Map<?, ?> dayMap) {
                            Map<String, Object> copy = new HashMap<>((Map<String, Object>) dayMap);
                            copy.put("day", dayNum++);
                            allItinerary.add(copy);
                        }
                    }
                }
                if (chunkResult.get("places") instanceof List<?> p) allPlaces.addAll(p);
                if (chunkResult.get("hotels") instanceof List<?> h) allHotels.addAll(h);
                if (chunkResult.get("transportation") instanceof List<?> t) {
                    t.forEach(x -> allTransportation.add(x.toString()));
                }
                if (chunkResult.get("costs") instanceof List<?> c) allCosts.addAll(c);

                chunkStart = chunkEnd.plusDays(1);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("places", allPlaces);
            result.put("hotels", allHotels);
            result.put("transportation", new ArrayList<>(allTransportation));
            result.put("costs", allCosts);
            result.put("itinerary", allItinerary);
            return result;
        }

        // Single chunk (<=7 days)
        return generateItineraryChunk(destination, travelers, startDate, endDate,
                preferences, budget);
    }
}