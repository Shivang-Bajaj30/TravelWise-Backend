package travelwise.backend.Models;


import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "testing")
public class Trips {

    @Id
    private String id;

    private String location;

    private Integer travelers;

    @Field("start_date")
    private String startDate;

    @Field("end_date")
    private String endDate;

    private String preferences;

    @Field("itinerary_data")
    private String itineraryData;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

}
