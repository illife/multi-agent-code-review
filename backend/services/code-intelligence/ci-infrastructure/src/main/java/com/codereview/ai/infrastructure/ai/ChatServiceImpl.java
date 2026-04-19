package com.codereview.ai.infrastructure.ai;

import com.codereview.ai.domain.service.AiService;
import com.codereview.ai.infrastructure.ai.chat.ChatProvider;
import org.springframework.stereotype.Service;

/**
 * Implementation of AiService interface using ChatProvider.
 * This adapter bridges the domain layer interface with infrastructure implementation.
 */
@Service
public class ChatServiceImpl implements AiService {

    private final ChatProvider chatProvider;

    public ChatServiceImpl(ChatProvider chatProvider) {
        this.chatProvider = chatProvider;
    }

    @Override
    public String chatCompletion(String systemMessage, String userMessage) {
        return chatCompletionWithContext(systemMessage, userMessage, null);
    }

    @Override
    public String chatCompletionWithContext(String systemMessage, String userMessage, String context) {
        String fullMessage = systemMessage + "\n\n" + userMessage;
        if (context != null && !context.isEmpty()) {
            fullMessage += "\n\nContext:\n" + context;
        }
        return chatProvider.generateAnswer(fullMessage, context);
    }

    @Override
    public String analyzeCode(String code, String language) {
        String prompt = buildPrompt(code, language);
        return chatProvider.generateAnswer(prompt, null);
    }

    private String buildPrompt(String code, String language) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please review the following code:\n\n");
        prompt.append("Language: ").append(language).append("\n");
        prompt.append("Code:\n").append(code).append("\n");
        return prompt.toString();
    }
}
