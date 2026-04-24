package travelwise.backend.Repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import travelwise.backend.Models.Trips;

public interface TripRepo extends MongoRepository<Trips, Integer> {

}
