package com.library.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "library.cache.book")
public record CacheProperties(
        @NotBlank String keyPrefix,
        @NotBlank String nullValue,
        @NotNull Duration ttl,
        @NotNull Duration nullTtl) {

    public CacheProperties {
        if (ttl != null && (ttl.isZero() || ttl.isNegative())) {
            throw new IllegalArgumentException("library.cache.book.ttl 必须大于0");
        }
        if (nullTtl != null && (nullTtl.isZero() || nullTtl.isNegative())) {
            throw new IllegalArgumentException("library.cache.book.null-ttl 必须大于0");
        }
    }
}
