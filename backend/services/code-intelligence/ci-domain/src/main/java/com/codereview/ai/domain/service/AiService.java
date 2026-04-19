package com.codereview.ai.domain.service;

/**
 * AI Service interface for domain layer.
 * Infrastructure layer provides the implementation.
 */
public interface AiService {

    /**
     * Send a chat completion request to the AI provider.
     *
     * @param systemMessage the system message
     * @param userMessage the user message
     * @return the AI response
     */
    String chatCompletion(String systemMessage, String userMessage);

    /**
     * Send a chat completion request with context.
     *
     * @param systemMessage the system message
     * @param userMessage the user message
     * @param context the additional context
     * @return the AI response
     */
    String chatCompletionWithContext(String systemMessage, String userMessage, String context);

    /**
     * Process a code review request using AI.
     *
     * @param code the code to analyze
     * @param language the programming language
     * @return the AI analysis result
     */
    String analyzeCode(String code, String language);
}
