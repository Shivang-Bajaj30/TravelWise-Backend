package travelwise.backend.config;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import travelwise.backend.Models.Trips;
import java.time.LocalDateTime;

public class TripsEventListener extends AbstractMongoEventListener<Trips> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Trips> event) {
        Trips trip = event.getSource();

        if (trip.getCreatedAt() == null) {
            trip.setCreatedAt(LocalDateTime.now());
        }
        trip.setUpdatedAt(LocalDateTime.now());
    }
}
