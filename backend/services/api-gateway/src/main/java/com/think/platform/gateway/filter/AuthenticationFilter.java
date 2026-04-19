package com.think.platform.gateway.filter;

import com.think.platform.shared.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Authentication Filter
 *
 * Validates JWT tokens and adds user information to request headers
 *
 * @author Platform Team
 */
@Slf4j
@Component
public class AuthenticationFilter implements GatewayFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    // JWT密钥 - 从配置文件读取
    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 不需要认证的路径
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/validate",
            "/api/health",
            "/actuator",
            "/doc.html",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Authentication filter processing: {}", path);

        // 检查是否是排除路径
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return chain.filter(exchange);
            }
        }

        // 检查OPTIONS请求（CORS预检）
        if ("OPTIONS".equals(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // 获取Authorization头
        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header: {}", path);
            return unauthorized(exchange);
        }

        // 提取token
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 验证token
            Claims claims = validateToken(token);

            // 添加用户信息到请求头
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(USER_ID_HEADER, claims.getSubject())
                    .header(USERNAME_HEADER, claims.get("username", String.class))
                    .header(USER_ROLE_HEADER, claims.get("role", String.class))
                    .build();

            exchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    /**
     * 验证JWT token
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(401, "未登录或登录已过期");
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(serialize(result));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 序列化对象为字节数组
     */
    private byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Serialization error", e);
            return "{\"code\":401,\"message\":\"未登录或登录已过期\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
