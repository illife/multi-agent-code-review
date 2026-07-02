package com.company.kb.core.service.impl;

import com.company.kb.core.service.ChatService;
import com.company.kb.infra.ai.chat.ChatProvider;
import com.think.platform.shared.infra.ai.AiUsageLimitExceededException;
import com.think.platform.shared.infra.ai.AiUsageLimiter;
import com.think.platform.shared.infra.ai.AiUsagePrincipalResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
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

    @Autowired(required = false)
    @Nullable
    private AiUsageLimiter aiUsageLimiter;

    @Value("${ai.usage-limit.chat-output-token-reserve:2000}")
    private int outputTokenReserve;

    @Override
    public String generateAnswer(String question, String context) throws Exception {
        log.info("Generating answer for question: {}", question);

        // Build RAG prompt
        String prompt = buildRAGPrompt(question, context);

        checkQuota("knowledge-chat", question, prompt);

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

        checkQuota("knowledge-chat-stream", question, prompt);

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

    private void checkQuota(String feature, String question, String prompt) {
        if (aiUsageLimiter == null) {
            return;
        }

        int estimatedTokens = AiUsageLimiter.estimateTokens(question)
                + AiUsageLimiter.estimateTokens(prompt)
                + outputTokenReserve;
        try {
            aiUsageLimiter.checkAndConsume(AiUsagePrincipalResolver.currentPrincipal(), feature, estimatedTokens);
        } catch (AiUsageLimitExceededException e) {
            log.warn("Knowledge chat blocked by AI usage limiter: feature={}, message={}", feature, e.getMessage());
            throw e;
        }
    }
}
