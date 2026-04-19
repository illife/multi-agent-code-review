package com.think.platform.shared.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket Question Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    /**
     * Request type: ask, heartbeat
     */
    private String type;

    /**
     * Question text
     */
    private String question;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * User ID (parsed from token)
     */
    private Long userId;

    /**
     * Timestamp (client-side time)
     */
    private Long timestamp;
}
