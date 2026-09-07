package com.library.security;

import com.library.config.properties.JwtProperties;
import com.library.model.entity.Role;
import com.library.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {
    private static final int HS256_MINIMUM_KEY_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < HS256_MINIMUM_KEY_BYTES) {
            throw new IllegalStateException("library.jwt.secret（JWT_SECRET）至少需要32字节，才能安全使用HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(properties.expiration());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthenticatedUser toPrincipal(Claims claims) {
        Number userId = claims.get("userId", Number.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        if (userId == null || username == null || username.isBlank() || role == null) {
            throw new IllegalArgumentException("JWT缺少必要身份信息");
        }
        return new AuthenticatedUser(userId.intValue(), username, Role.valueOf(role));
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }
}
