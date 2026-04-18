package com.vsrna.backend.infrastructure.security;

import com.vsrna.backend.domain.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expirationHours;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:24}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(UUID userId, Set<String> roles) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + expirationHours * 3_600_000L))
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiException.unauthorized("invalid or expired token");
        }
    }
}
