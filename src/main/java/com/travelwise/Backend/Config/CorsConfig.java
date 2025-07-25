package com.travelwise.Backend.Config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:4200,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Split the comma-separated string into an array of origins
                // This handles cases where you have multiple allowed origins (e.g., dev & prod)
                String[] originsArray = allowedOrigins.split(",");

                registry.addMapping("/**") // Apply CORS to all API endpoints
                        .allowedOrigins(originsArray) // Specify allowed origins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Specify allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true) // Allow cookies, authorization headers etc.
                        .maxAge(3600); // How long the preflight request can be cached (in seconds)
            }
        };
    }
}