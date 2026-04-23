package travelwise.backend.controller;

import travelwise.backend.dto.Login;
import travelwise.backend.dto.Signup;
import travelwise.backend.Models.User;
import travelwise.backend.Repo.UserRepo;
import travelwise.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @GetMapping("/")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok(Map.of("message",
            "Spring Boot backend is running and connected to SQL Server! 🚀"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Signup req) {
        if (req.getName() == null || req.getEmail() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepo.save(user);

        return ResponseEntity.status(201).body(Map.of("message", "Signup successful!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login req) {
        var userOpt = userRepo.findByEmail(req.getEmail());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        User user = userOpt.get();
        String token = jwtUtil.generateToken(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName()
        ));
        return ResponseEntity.ok(Map.of(
                "message", "Login successful ✅",
                "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail()),
                "token", token
        ));
    }
}
