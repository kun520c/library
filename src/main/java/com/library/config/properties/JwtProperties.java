package com.library.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "library.jwt")
public record JwtProperties(
        @NotBlank(message = "library.jwt.secret（JWT_SECRET）不能为空") String secret,
        @NotNull(message = "library.jwt.expiration 不能为空") Duration expiration) {

    public JwtProperties {
        if (expiration != null && (expiration.isZero() || expiration.isNegative())) {
            throw new IllegalArgumentException("library.jwt.expiration 必须大于0");
        }
    }
}
