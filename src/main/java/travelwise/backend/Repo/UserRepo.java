package travelwise.backend.Repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import travelwise.backend.Models.User;
import java.util.Optional;

public interface UserRepo extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByname(String name);
}
