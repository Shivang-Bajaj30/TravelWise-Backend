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
            throw new RuntimeException("Email Already Exists!");
        }
        return userRepo.save(user);
    }
}
