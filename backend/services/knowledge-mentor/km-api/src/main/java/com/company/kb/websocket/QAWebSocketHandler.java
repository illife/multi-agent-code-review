package com.company.kb.websocket;

import com.company.kb.core.service.QAService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket问答处理器
 *
 * 处理客户端的WebSocket连接，实现流式问答输出
 * TODO: Add JWT authentication
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QAWebSocketHandler extends TextWebSocketHandler {

    private final QAService qaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
        // TODO: Extract and validate user from JWT token
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String question = (String) payload.get("question");
            String userId = (String) payload.getOrDefault("userId", "anonymous");

            log.info("收到问答请求: sessionId={}, question={}", session.getId(), question);

            // 流式处理问答
            qaService.processQueryWithStream(question, userId, new QAService.StreamCallback() {
                @Override
                public void onToken(String token) {
                    try {
                        Map<String, String> response = new HashMap<>();
                        response.put("type", "token");
                        response.put("content", token);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("发送token失败", e);
                    }
                }

                @Override
                public void onComplete(java.util.List<Map<String, Object>> sources) {
                    try {
                        Map<String, Object> response = new HashMap<>();
                        response.put("type", "complete");
                        response.put("sources", sources);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("发送完成消息失败", e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    try {
                        Map<String, String> response = new HashMap<>();
                        response.put("type", "error");
                        response.put("message", error.getMessage());
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("发送错误消息失败", e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("处理问答请求失败: sessionId={}", session.getId(), e);
            sendError(session, "处理请求失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("type", "error");
            response.put("message", message);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
}
