package com.chethu.paymentledgerservice.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(normalizeEmail(user.getEmail()))
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return claims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object value = claims(token).get(CLAIM_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        return null;
    }

    public UserRole extractRole(String token) {
        Object value = claims(token).get(CLAIM_ROLE);
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.toString());
    }

    public Instant extractExpiration(String token) {
        Date expiration = claims(token).getExpiration();
        return expiration == null ? null : expiration.toInstant();
    }

    public boolean isTokenSignatureAndStructureValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        Instant expiration = extractExpiration(token);
        return expiration != null && Instant.now().isAfter(expiration);
    }

    public boolean belongsToUser(String token, UserEntity user) {
        if (user == null || !isTokenSignatureAndStructureValid(token)) {
            return false;
        }
        String normalizedEmail = normalizeEmail(user.getEmail());
        return normalizedEmail.equals(extractSubject(token))
                && user.getId() != null
                && user.getId().equals(extractUserId(token))
                && user.getRole() == extractRole(token)
                && !isTokenExpired(token);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    private Claims claims(String token) {
        try {
            return parseClaims(token).getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
