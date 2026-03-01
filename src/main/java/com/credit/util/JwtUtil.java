package com.credit.util;

import com.credit.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSigningKey().getBytes(StandardCharsets.UTF_8));
    }

    private JwtParserBuilder baseParser() {
        JwtParserBuilder builder = Jwts.parser()
                .verifyWith(getSigningKey())
                .clockSkewSeconds(60);
        if (jwtProperties.getIssuer() != null && !jwtProperties.getIssuer().isBlank()) {
            builder.requireIssuer(jwtProperties.getIssuer());
        }
        if (jwtProperties.getAudience() != null && !jwtProperties.getAudience().isBlank()) {
            builder.requireAudience(jwtProperties.getAudience());
        }
        return builder;
    }

    public String getUserIdFromToken(String token) {
        try {
            Claims claims = baseParser()
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token: " + e.getMessage());
        }
    }

    public String getUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return getUserIdFromToken(token);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getRolesFromToken(String token) {
        try {
            Claims claims = baseParser()
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object role = claims.get("role");
            if (role instanceof java.util.List) {
                return (java.util.List<String>) role;
            } else if (role instanceof String) {
                return java.util.List.of((String) role);
            }
            return java.util.List.of();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token: " + e.getMessage());
        }
    }

    public java.util.List<String> getRolesFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return getRolesFromToken(authHeader.substring(7));
    }
}
