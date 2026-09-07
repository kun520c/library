package com.library.security;

import com.library.config.properties.JwtProperties;
import com.library.model.entity.Role;
import com.library.model.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "test-only-jwt-secret-at-least-32-bytes-long";
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(12)));
        user = new User();
        user.setId(7);
        user.setUsername("reader");
        user.setRole(Role.USER);
    }

    @Test
    void generatesAndParsesTokenWithRequiredClaims() {
        String token = jwtService.generateToken(user);
        var claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("userId", Number.class).intValue()).isEqualTo(7);
        assertThat(claims.get("username", String.class)).isEqualTo("reader");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getHeader()
                .getAlgorithm()).isEqualTo("HS256");
    }

    @Test
    void rejectsTokenSignedByAnotherKey() {
        JwtService anotherService = new JwtService(new JwtProperties(
                "another-test-secret-that-is-at-least-32-bytes", Duration.ofHours(12)));

        assertThatThrownBy(() -> jwtService.parseToken(anotherService.generateToken(user)))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = Jwts.builder()
                .subject("7")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseToken(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(user);
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOfAny(SignatureException.class, io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void rejectsShortSecretAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("至少需要32字节");
    }

    @Test
    void rejectsEmptySecretAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("", Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("至少需要32字节");
    }
}
