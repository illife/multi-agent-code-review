package com.codereview.ai.infrastructure.ai.chat;

import java.util.List;
import java.util.Map;

/**
 * Chat Provider Interface
 *
 * Abstraction for AI chat completion providers
 * Extended to support Function Calling for ReAct Agent
 *
 * @author Code Review AI Team
 */
public interface ChatProvider {

    /**
     * Generate a complete answer
     *
     * @param question User question/prompt
     * @param context Additional context
     * @return Generated answer
     */
    String generateAnswer(String question, String context);

    /**
     * Stream generated answer
     *
     * @param question User question/prompt
     * @param context Additional context
     * @param callback Callback for streaming tokens
     */
    void streamAnswer(String question, String context, StreamCallback callback);

    /**
     * Chat with Function Calling support
     *
     * @param userMessage User message
     * @param history Conversation history
     * @param tools Available tools for AI to call
     * @param handler Handler for tool execution
     * @return AI response (may include tool call results)
     */
    String chatWithTools(String userMessage,
                         List<ChatMessage> history,
                         List<ToolDefinition> tools,
                         ToolCallHandler handler);

    /**
     * Streaming chat with Function Calling
     *
     * @param userMessage User message
     * @param history Conversation history
     * @param tools Available tools
     * @param handler Tool call handler
     * @param callback Streaming callback
     */
    void streamChatWithTools(String userMessage,
                             List<ChatMessage> history,
                             List<ToolDefinition> tools,
                             ToolCallHandler handler,
                             StreamCallback callback);

    // ========================================================================
    // Inner Classes & Interfaces
    // ========================================================================

    /**
     * Chat Message for conversation history
     */
    record ChatMessage(String role, String content) {
    }

    /**
     * Tool Definition for Function Calling
     */
    record ToolDefinition(String name, String description, String parameters) {
    }

    /**
     * Tool Call Information
     */
    record ToolCall(String name, Map<String, Object> arguments) {
    }

    /**
     * Tool Call Handler - executed when AI requests to call a tool
     */
    interface ToolCallHandler {
        /**
         * Handle tool call from AI
         *
         * @param toolCall Tool name and arguments
         * @return Tool execution result (will be fed back to AI)
         */
        String onToolCall(ToolCall toolCall);
    }

    /**
     * Streaming callback interface
     */
    interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Throwable error);
    }
}
