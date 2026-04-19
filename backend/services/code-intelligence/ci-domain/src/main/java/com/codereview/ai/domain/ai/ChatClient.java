package com.codereview.ai.domain.ai;

import java.util.List;
import java.util.Map;

/**
 * Chat Client Interface - Domain Layer
 *
 * Abstract interface for AI chat services
 * Implemented by infrastructure layer (QwenChatProvider)
 *
 * @author Code Review AI Team
 */
public interface ChatClient {

    /**
     * Chat with function calling support
     */
    String chatWithTools(String userMessage,
                         List<ChatMessage> history,
                         List<ToolDefinition> tools,
                         ToolCallHandler handler);

    /**
     * Chat message
     */
    record ChatMessage(String role, String content) {}

    /**
     * Tool definition
     */
    record ToolDefinition(String name, String description, String parameters) {}

    /**
     * Tool call
     */
    record ToolCall(String name, Map<String, Object> arguments) {}

    /**
     * Tool call handler
     */
    interface ToolCallHandler {
        String onToolCall(ToolCall toolCall);
    }
}
