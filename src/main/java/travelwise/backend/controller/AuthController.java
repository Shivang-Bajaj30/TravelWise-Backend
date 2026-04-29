package travelwise.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
import java.util.Optional;

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
        String identifier = req.getIdentifier();
        String password = req.getPassword();

        // Try to find user by email first
        Optional<User> userOpt = userRepo.findByEmail(identifier);

        // If not found by email, try to find by username
        if (userOpt.isEmpty()) {
            userOpt = userRepo.findByname(identifier);
        }

        // If user not found or password doesn't match
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
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

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 1. Check if token is present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized: No token provided"));
        }

        try {
            // 2. Extract and parse the token
            String token = authHeader.substring(7); // Removes "Bearer "
            var claims = jwtUtil.parseToken(token);

            // 3. Get the email from the token claims
            String email = claims.get("email", String.class);

            // 4. Find the user
            var user = userRepo.findByEmail(email).orElse(null);

            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            // 5. If token is expired or invalid, parseToken() throws an error which we catch here
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
        }
    }

}
