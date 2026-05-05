package travelwise.backend.controller;

import org.springframework.http.HttpStatus;
import travelwise.backend.dto.LoginRequest;
import travelwise.backend.Repo.UserRepo;
import travelwise.backend.dto.SignupRequest;
import travelwise.backend.dto.UserDTO;
import travelwise.backend.Models.User;
import travelwise.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req.getName() == null || req.getName().isBlank() ||
                req.getEmail() == null || req.getEmail().isBlank() ||
                req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        if (req.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepo.save(user);

        return ResponseEntity.status(201).body(Map.of("message", "Signup successful!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String identifier = req.getIdentifier();
        String password = req.getPassword();

        if (req.getIdentifier() == null || req.getIdentifier().isBlank() ||
                req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email/username and password are required"));
        }

        // Try to find user by email first
        Optional<User> userOpt = userRepo.findByEmail(identifier);

        // If not found by email, try to find by username
        if (userOpt.isEmpty()) {
            userOpt = userRepo.findByname(identifier);
        }

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
                "user", UserDTO.fromUser(user),
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
                return ResponseEntity.ok(UserDTO.fromUser(user));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            // 5. If token is expired or invalid, parseToken() throws an error which we catch here
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
        }
    }

    private String getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        var claims = jwtUtil.parseToken(token);
        return claims.get("id", String.class);
    }

    @PutMapping("/users/update")
    public ResponseEntity<?> updateProfile( @RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestBody Map<String, String> updates) {
        try {
            String userId = getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }
            // 2. Find the user in the database
            Optional<User> optionalUser = userRepo.findById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            User user = optionalUser.get();

            if (updates.containsKey("name") && !updates.get("name").isBlank()) {
                user.setName(updates.get("name"));
            }

            if (updates.containsKey("email") && !updates.get("email").isBlank()) {
                user.setEmail(updates.get("email"));
            }

            user.setUpdatedAt(Instant.now());

            userRepo.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully ✅",
                    "user", UserDTO.fromUser(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while updating the profile"));
        }
    }
}
