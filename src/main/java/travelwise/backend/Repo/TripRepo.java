package travelwise.backend.Repo;


import travelwise.backend.Models.Trips;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepo extends JpaRepository<Trips, Integer> {
}
