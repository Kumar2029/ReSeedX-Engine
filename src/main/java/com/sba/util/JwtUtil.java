package com.sba.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:SeedBlockAlgorithmPhase3SecretKey2026!XOR}")
    private String secret;

    @Value("${jwt.expiry-ms:86400000}")
    private long expiryMs;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String clientId, String username, String role) {
        return Jwts.builder()
            .setSubject(clientId)
            .claim("username", username)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public String extractClientId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return (String) extractClaims(token).get("username");
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    public boolean isValid(String token) {
        try { extractClaims(token); return true; }
        catch (Exception e) { return false; }
    }
}
