package com.knightdevelopers.kheerabackend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final String SECRET = "test-secret-for-jwt-unit-tests-1234567890";
    private static final Instant NOW = Instant.parse("2026-09-12T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final AuthenticationService authenticationService =
            new AuthenticationService(SECRET, 86_400_000L, CLOCK);

    @Test
    void generateTokenCreatesValidTokenWithEmailSubject() {
        String token = authenticationService.generateToken("member@example.com");

        assertThat(authenticationService.validateToken(token)).isTrue();
        assertThat(authenticationService.getEmailFromToken(token)).isEqualTo("member@example.com");
    }

    @Test
    void validateTokenRejectsAlteredToken() {
        String token = authenticationService.generateToken("member@example.com");

        assertThat(authenticationService.validateToken(token + "x")).isFalse();
    }

    @Test
    void validateTokenRejectsTokenSignedWithDifferentSecret() {
        String token = Jwts.builder()
                .setSubject("member@example.com")
                .setIssuedAt(Date.from(NOW))
                .setExpiration(Date.from(NOW.plusSeconds(60)))
                .signWith(
                        Keys.hmacShaKeyFor("different-secret-for-jwt-unit-tests-12345".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();

        assertThat(authenticationService.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        AuthenticationService shortLivedService = new AuthenticationService(SECRET, 1_000L, CLOCK);
        String token = shortLivedService.generateToken("member@example.com");
        AuthenticationService afterExpiryService = new AuthenticationService(
                SECRET,
                1_000L,
                Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC)
        );

        assertThat(afterExpiryService.validateToken(token)).isFalse();
    }

    @Test
    void getEmailFromTokenRejectsInvalidToken() {
        assertThatThrownBy(() -> authenticationService.getEmailFromToken("not-a-jwt"))
                .isInstanceOf(RuntimeException.class);
    }
}
