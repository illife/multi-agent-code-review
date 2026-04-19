package com.company.kb.core.service.impl;

import com.company.kb.core.service.ChatService;
import com.company.kb.infra.ai.chat.ChatProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Chat service implementation
 * Integrates with AI provider (Qwen) through infrastructure layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatProvider chatProvider;

    @Override
    public String generateAnswer(String question, String context) throws Exception {
        log.info("Generating answer for question: {}", question);

        // Build RAG prompt
        String prompt = buildRAGPrompt(question, context);

        // Generate answer using AI provider
        String answer = chatProvider.generateAnswer(question, prompt);

        log.info("Answer generated successfully, length: {}", answer.length());
        return answer;
    }

    @Override
    public void streamAnswer(String question, String context, StreamCallback callback) throws Exception {
        log.info("Streaming answer for question: {}", question);

        // Build RAG prompt
        String prompt = buildRAGPrompt(question, context);

        // Stream answer using AI provider
        chatProvider.streamAnswer(question, prompt, new ChatProvider.StreamCallback() {
            @Override
            public void onToken(String token) {
                try {
                    callback.onToken(token);
                } catch (Exception e) {
                    log.error("Error in stream callback onToken", e);
                }
            }

            @Override
            public void onComplete() {
                try {
                    callback.onComplete();
                } catch (Exception e) {
                    log.error("Error in stream callback onComplete", e);
                }
            }

            @Override
            public void onError(Throwable error) {
                try {
                    callback.onError(error);
                } catch (Exception e) {
                    log.error("Error in stream callback onError", e);
                }
            }
        });
    }

    /**
     * Build RAG (Retrieval-Augmented Generation) prompt
     */
    private String buildRAGPrompt(String question, String context) {
        if (context == null || context.trim().isEmpty()) {
            context = "No relevant context found. Please answer based on your general knowledge.";
        }

        return String.format("""
                You are a helpful AI assistant. Based on the following context, please answer the user's question accurately.

                ============ Context ============
                %s
                ============ End of Context ============

                Question: %s

                Instructions:
                1. Answer the question based primarily on the provided context.
                2. If the context doesn't contain relevant information, say so clearly.
                3. Be concise and direct.
                4. If you're unsure, admit it rather than making up information.

                Answer:
                """, context, question);
    }
}
