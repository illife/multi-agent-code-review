package com.think.platform.gateway.filter;

import com.think.platform.shared.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Admin Role Filter
 *
 * Checks if user has ADMIN role
 *
 * @author Platform Team
 */
@Slf4j
@Component
public class AdminRoleFilter implements GatewayFilter, Ordered {

    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String REQUIRED_ROLE = "ADMIN";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Admin role filter processing: {}", path);

        // 获取用户角色（由AuthenticationFilter添加）
        String userRole = request.getHeaders().getFirst(USER_ROLE_HEADER);

        if (userRole == null) {
            log.warn("Missing user role header");
            return forbidden(exchange);
        }

        if (!REQUIRED_ROLE.equalsIgnoreCase(userRole)) {
            log.warn("User does not have ADMIN role: {}", userRole);
            return forbidden(exchange);
        }

        return chain.filter(exchange);
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(403, "权限不足，需要管理员权限");
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
            return "{\"code\":403,\"message\":\"权限不足，需要管理员权限\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return -99; // 优先级低于AuthenticationFilter
    }
}
