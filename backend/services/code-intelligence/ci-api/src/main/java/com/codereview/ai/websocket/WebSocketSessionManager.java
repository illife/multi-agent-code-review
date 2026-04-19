package com.codereview.ai.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session Manager
 *
 * Manages active WebSocket sessions
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.debug("Added WebSocket session: userId={}, sessionId={}", userId, session.getId());
    }

    public void removeSession(String userId) {
        WebSocketSession session = sessions.remove(userId);
        if (session != null) {
            log.debug("Removed WebSocket session: userId={}, sessionId={}", userId, session.getId());
        }
    }

    public WebSocketSession getSession(String userId) {
        return sessions.get(userId);
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
