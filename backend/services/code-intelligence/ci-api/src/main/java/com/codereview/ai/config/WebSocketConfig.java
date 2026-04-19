package com.codereview.ai.config;

import com.codereview.ai.websocket.CodeReviewWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration
 *
 * Configures WebSocket endpoints for code review
 *
 * @author Code Review AI Team
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CodeReviewWebSocketHandler codeReviewWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(codeReviewWebSocketHandler, "/api/ws/review")
                .setAllowedOriginPatterns("*");
    }
}
