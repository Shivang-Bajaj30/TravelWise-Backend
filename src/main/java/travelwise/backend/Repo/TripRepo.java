package travelwise.backend.Repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import travelwise.backend.Models.Trips;

import java.util.List;

public interface TripRepo extends MongoRepository<Trips, String> {

    List<Trips> findByUserIdOrderByCreatedAtDesc(String userId);
}
