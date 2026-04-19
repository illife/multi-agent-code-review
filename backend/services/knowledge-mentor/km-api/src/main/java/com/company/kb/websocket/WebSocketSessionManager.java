package com.company.kb.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 * 管理所有活跃的WebSocket连接
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 存储用户ID到WebSocket会话的映射
     */
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
        log.info("WebSocket会话已添加: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 移除会话
     */
    public void removeSession(String userId) {
        WebSocketSession session = userSessions.remove(userId);
        if (session != null) {
            log.info("WebSocket会话已移除: userId={}, sessionId={}", userId, session.getId());
        }
    }

    /**
     * 获取会话
     */
    public WebSocketSession getSession(String userId) {
        return userSessions.get(userId);
    }

    /**
     * 发送消息到指定用户
     */
    public boolean sendMessage(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                return true;
            } catch (Exception e) {
                log.error("发送WebSocket消息失败: userId={}", userId, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 广播消息到所有用户
     */
    public void broadcast(String message) {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                } catch (Exception e) {
                    log.error("广播WebSocket消息失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
