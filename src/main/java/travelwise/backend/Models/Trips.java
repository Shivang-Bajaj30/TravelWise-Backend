package travelwise.backend.Models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Trips {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private Integer travelers;

    @Column(name = "start_date", nullable = false, length = 50)
    private String startDate;

    @Column(name = "end_date", nullable = false, length = 50)
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String preferences;

    @Column(name = "itinerary_data", columnDefinition = "TEXT")
    private String itineraryData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
