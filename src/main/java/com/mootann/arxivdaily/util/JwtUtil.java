package com.mootann.arxivdaily.util;

import com.mootann.arxivdaily.constant.JwtConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:arXivDailySecretKey2026ForJWTTokenGeneration}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role, Set<String> orgTags, String primaryOrg) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim(JwtConstant.CLAIM_USER_ID, userId)
                .claim(JwtConstant.CLAIM_USERNAME, username)
                .claim(JwtConstant.CLAIM_ROLE, role)
                .claim(JwtConstant.CLAIM_ORG_TAGS, orgTags)
                .claim(JwtConstant.CLAIM_PRIMARY_ORG, primaryOrg)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get(JwtConstant.CLAIM_USER_ID, Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get(JwtConstant.CLAIM_ROLE, String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractOrgTags(String token) {
        Object orgTags = extractClaims(token).get(JwtConstant.CLAIM_ORG_TAGS);
        if (orgTags instanceof java.util.Collection) {
            return new java.util.HashSet<>((java.util.Collection<String>) orgTags);
        }
        return java.util.Collections.emptySet();
    }

    public String extractPrimaryOrg(String token) {
        return extractClaims(token).get(JwtConstant.CLAIM_PRIMARY_ORG, String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    public Long getRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0L;
        }
    }
}
