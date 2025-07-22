package com.travelwise.Backend;

// Remove or comment out this import if Dotenv is no longer used anywhere else
// import io.github.cdimascio.dotenv.Dotenv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
	public static void main(String[] args) {
		// IMPORTANT: Remove or comment out the following line
		// Dotenv dotenv = Dotenv.load(); // This line caused the "Could not find /.env" error

		SpringApplication.run(BackendApplication.class, args);
	}
}