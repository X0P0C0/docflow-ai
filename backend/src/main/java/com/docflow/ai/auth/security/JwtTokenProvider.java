package com.docflow.ai.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-seconds}")
    private Long expireSeconds;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() >= 44) {
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (IllegalArgumentException ignored) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Long userId, String username, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return Long.parseLong(String.valueOf(userId));
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }
}
