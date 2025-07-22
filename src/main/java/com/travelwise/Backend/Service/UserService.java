package com.travelwise.Backend.Service;

import com.travelwise.Backend.Model.User;
import com.travelwise.Backend.Repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public User registerUser(User user) {
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        return userRepo.save(user);
    }

    public User login(String email, String password) {
        return userRepo.findByEmail(email)
                .map(user -> {
                    if (!user.getPassword().equals(password)) {
                        throw new RuntimeException("Incorrect password");
                    }
                    return user;
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
