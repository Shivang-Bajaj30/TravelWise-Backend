package travelwise.backend.Repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import travelwise.backend.Models.Packages;

public interface PackageRepo extends MongoRepository<Packages, Integer> {
}
