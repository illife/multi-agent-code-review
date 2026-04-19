package com.think.platform.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global Exception Handler for Gateway
 *
 * Handles exceptions and returns standardized error responses
 *
 * @author Platform Team
 */
@Slf4j
@Component
public class GatewayExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(org.springframework.web.server.ServerWebExchange exchange, Throwable ex) {
        ServerWebExchange swe = (ServerWebExchange) exchange;

        HttpStatus status;
        String message;

        if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "请求的资源不存在";
        } else if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "服务内部错误";
            log.error("Gateway error: {}", ex.getMessage(), ex);
        }

        swe.getResponse().setStatusCode(status);
        swe.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(status.value(), message);
        DataBuffer buffer = swe.getResponse().bufferFactory().wrap(serialize(errorResponse));

        return swe.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Serialization error", e);
            return ("{\"code\":" + HttpStatus.INTERNAL_SERVER_ERROR.value() + ",\"message\":\"序列化错误\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Error response DTO
     */
    record ErrorResponse(int code, String message) {}
}
