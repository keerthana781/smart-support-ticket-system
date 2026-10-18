package com.supportdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Type-safe view of the "app.*" settings in application.yml.
 * Spring binds the YAML (and environment variables) into these records at startup.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed, Ai ai) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Seed(boolean enabled) {
    }

    public record Ai(String provider, Gemini gemini) {
    }

    public record Gemini(String apiKey, String model, String baseUrl, int timeoutSeconds) {
    }
}
