package com.library.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "library.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
    public CorsProperties {
        if (allowedOrigins != null && allowedOrigins.stream().anyMatch(origin -> origin == null
                || origin.isBlank()
                || (!origin.startsWith("http://") && !origin.startsWith("https://")))) {
            throw new IllegalArgumentException("library.cors.allowed-origins 只能包含明确的 http/https 来源");
        }
    }
}
