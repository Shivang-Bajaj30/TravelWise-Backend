package travelwise.backend.Models;

import lombok.Data;

@Data
public class ItineraryRequest {
    private String destination;
    private String location;  // alias for destination
    private Integer travelers;
    private String startDate;
    private String endDate;
    private String preferences;
    private String budget;
    private String travelWith;

    public String getEffectiveDestination() {
        return destination != null ? destination : location;
    }
}
