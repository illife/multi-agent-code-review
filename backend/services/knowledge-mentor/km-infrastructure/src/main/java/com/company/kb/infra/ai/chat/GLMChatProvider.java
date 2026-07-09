package com.company.kb.infra.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.infra.ai.AiUsageAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 智谱对话生成提供商
 * 实现智谱AI的对话生成功能
 */
@Slf4j
@Component
public class GLMChatProvider implements ChatProvider {

    @Value("${glm.api-url}")
    private String apiUrl;

    @Value("${glm.api-key}")
    private String apiKey;

    @Value("${glm.chat-model:glm-4-flash}")
    private String chatModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Nullable
    private final AiUsageAuditService aiUsageAuditService;

    public GLMChatProvider(@Nullable AiUsageAuditService aiUsageAuditService) {
        this.aiUsageAuditService = aiUsageAuditService;
    }

    @Override
    public String generateAnswer(String question, String context) throws Exception {
        long startTime = System.currentTimeMillis();
        log.debug("智谱生成答案: questionLength={}, contextLength={}",
            question.length(), context.length());

        String prompt = buildPrompt(question, context);
        String url = apiUrl + "/chat/completions";

        // 构建请求体
        String requestBody = String.format(
            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7}",
            chatModel,
            prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );

        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            String answer = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode usage = root.path("usage");
            recordAudit(question, context, answer.length(), usage, System.currentTimeMillis() - startTime, true);

            log.debug("智谱答案生成成功: answerLength={}", answer.length());
            return answer;
        } catch (Exception e) {
            recordAudit(question, context, 0, null, System.currentTimeMillis() - startTime, false);
            throw e;
        }
    }

    @Override
    public void streamAnswer(String question, String context, StreamCallback callback) throws Exception {
        log.debug("智谱流式生成答案: questionLength={}, contextLength={}",
            question.length(), context.length());

        // 注意：GLM的流式实现需要使用流式HTTP客户端
        // 简化起见，先用非流式实现
        String answer = generateAnswer(question, context);
        callback.onToken(answer);
        callback.onComplete();
    }

    @Override
    public String getProviderName() {
        return "glm";
    }

    /**
     * 构建RAG Prompt
     */
    private String buildPrompt(String question, String context) {
        return String.format(
            "请根据以下上下文信息回答用户的问题。如果上下文中没有相关信息，请明确告知用户。\n\n" +
            "上下文信息：\n%s\n\n" +
            "用户问题：%s\n\n" +
            "请基于上下文信息回答问题，并在回答中引用来源。",
            context, question
        );
    }

    private void recordAudit(String question,
                             String context,
                             int answerChars,
                             JsonNode usage,
                             long durationMs,
                             boolean success) {
        if (aiUsageAuditService == null) {
            return;
        }

        long promptTokens = usage != null ? usage.path("prompt_tokens").asLong(0) : 0;
        long completionTokens = usage != null ? usage.path("completion_tokens").asLong(0) : 0;
        long totalTokens = usage != null ? usage.path("total_tokens").asLong(0) : 0;
        if (promptTokens == 0) {
            promptTokens = AiUsageAuditService.estimateTokens(question) + AiUsageAuditService.estimateTokens(context);
        }
        if (completionTokens == 0 && answerChars > 0) {
            completionTokens = AiUsageAuditService.estimateTokens("x".repeat(answerChars));
        }
        if (totalTokens == 0) {
            totalTokens = promptTokens + completionTokens;
        }

        aiUsageAuditService.record(AiUsageAuditService.AiUsageAuditEvent.builder()
            .provider("glm")
            .model(chatModel)
            .feature("knowledge-chat")
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .totalTokens(totalTokens)
            .durationMs(durationMs)
            .success(success)
            .build());
    }
}
