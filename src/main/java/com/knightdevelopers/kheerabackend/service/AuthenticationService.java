package com.knightdevelopers.kheerabackend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class AuthenticationService {
    private final String secretKey;
    private final long tokenExpirationMillis;
    private final Clock clock;

    public AuthenticationService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-millis:86400000}") long tokenExpirationMillis,
            Clock clock
    ) {
        this.secretKey = secretKey;
        this.tokenExpirationMillis = tokenExpirationMillis;
        this.clock = clock;
    }

    public String generateToken(String email){

        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Instant issuedAt = clock.instant();

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(issuedAt.plusMillis(tokenExpirationMillis)))
                .signWith(key,SignatureAlgorithm.HS256)
                .compact();
    }
    public boolean validateToken(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(token);  // will throw exception if invalid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .setClock(() -> Date.from(clock.instant()))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

}
