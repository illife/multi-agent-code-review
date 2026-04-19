package com.company.kb.config;

import com.company.kb.websocket.QAWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置QA问答的WebSocket端点
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final QAWebSocketHandler qaWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // QA问答WebSocket端点
        registry.addHandler(qaWebSocketHandler, "/ws/qa")
            .setAllowedOrigins("*");  // 允许所有源用于测试
    }
}
