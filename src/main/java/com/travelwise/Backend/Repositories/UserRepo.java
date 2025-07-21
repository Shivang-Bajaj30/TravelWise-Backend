package com.travelwise.Backend.Repositories;

import com.travelwise.Backend.Model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepo extends MongoRepository<User, String> {
    boolean existsByEmail(String email);
}
