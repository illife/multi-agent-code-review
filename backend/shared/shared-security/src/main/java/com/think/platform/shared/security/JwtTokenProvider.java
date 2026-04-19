package com.think.platform.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider
 * Merged from codereview-api and knowledge-base-api
 *
 * 负责 JWT token 的生成、解析和验证
 * 支持 Access Token 和 Refresh Token
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
@Getter
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")  // 默认 24 小时
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")  // 默认 7 天
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtTokenProvider initialized with {}ms access token expiration", accessTokenExpirationMs);
    }

    /**
     * 生成 Access Token (用于用户ID和用户名)
     */
    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 生成 Access Token (用于 Authentication 对象)
     */
    public String generateAccessToken(Authentication authentication) {
        SharedUserDetails userDetails = (SharedUserDetails) authentication.getPrincipal();
        return generateAccessToken(userDetails.getId(), userDetails.getUsername());
    }

    /**
     * 生成 Refresh Token (仅包含用户ID)
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 生成 Refresh Token (用于 Authentication 对象)
     */
    public String generateRefreshToken(Authentication authentication) {
        SharedUserDetails userDetails = (SharedUserDetails) authentication.getPrincipal();
        return generateRefreshToken(userDetails.getId());
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            log.error("Failed to extract userId from token", e);
            return null;
        }
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("Failed to extract username from token", e);
            return null;
        }
    }

    /**
     * 从 Token 中获取签发时间
     */
    public long getIssuedAtFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getIssuedAt().getTime();
        } catch (Exception e) {
            log.error("Failed to extract issuedAt from token", e);
            return 0;
        }
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Failed to get remaining expiration", e);
            return 0;
        }
    }

    /**
     * 检查 Token 是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            log.debug("JWT token validated successfully");
            return true;
        } catch (SecurityException ex) {
            log.error("JWT signature validation failed: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("JWT malformed: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("JWT unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * 从 Authorization header 中提取 token
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * 解析 Token (内部方法)
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
