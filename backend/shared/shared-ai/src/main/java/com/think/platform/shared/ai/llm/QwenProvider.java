package com.think.platform.shared.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.infra.ai.AiUsageAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Qwen AI Provider (通义千问)
 * 统一的 Qwen API 调用实现
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class QwenProvider implements LlmProvider {

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String apiUrl;

    @Value("${qwen.chat-model:qwen3.6-flash}")
    private String chatModel;

    @Value("${qwen.chat-models:}")
    private String chatModels;

    @Value("${qwen.allowed-chat-models:qwen3.6-flash}")
    private String allowedChatModels;

    @Value("${qwen.embedding-model:text-embedding-v4}")
    private String embeddingModel;

    @Value("${qwen.max-tokens:4000}")
    private int maxTokens;

    @Value("${qwen.temperature:0.7}")
    private double temperature;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<String> chatModelPool = List.of();
    private Set<String> allowedChatModelSet = Set.of();

    @Autowired(required = false)
    @Nullable
    private AiUsageAuditService aiUsageAuditService;

    public QwenProvider() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.allowedChatModelSet = parseAllowedModelSet(allowedChatModels);
        this.chatModelPool = parseModelPool(chatModels);
        log.info("Qwen shared allowed chat models: {}", allowedChatModelSet);
        log.info("Qwen shared chat model pool: {}", chatModelPool.isEmpty() ? "disabled" : chatModelPool);
        if (!chatModelPool.isEmpty()) {
            log.info("Qwen shared non-streaming chat model pool: {}", compatibleNonStreamingModels());
        }
    }

    @Override
    public String getName() {
        return "Qwen";
    }

    @Override
    public LlmProviderType getType() {
        return LlmProviderType.QWEN;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String selectedModel = null;
        try {
            Map<String, Object> requestBody = buildChatBody(request);
            selectedModel = String.valueOf(requestBody.get("model"));

            String response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                    .block();

            ChatResponse chatResponse = parseChatResponse(response);
            recordChatAudit(request, chatResponse, selectedModel, System.currentTimeMillis() - startTime);
            return chatResponse;
        } catch (Exception e) {
            log.error("Qwen chat failed", e);
            recordChatFailure(request, selectedModel, System.currentTimeMillis() - startTime);
            return ChatResponse.failure("Qwen API 调用失败: " + e.getMessage());
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", texts);
            requestBody.put("parameters", Map.of("text_type", "document"));

            String response = webClient.post()
                    .uri(apiUrl + "/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<float[]> embeddings = parseEmbeddingResponse(response);
            recordEmbeddingAudit(texts, response, true, System.currentTimeMillis() - startTime);
            return embeddings;
        } catch (Exception e) {
            log.error("Qwen embedding failed", e);
            recordEmbeddingAudit(texts, null, false, System.currentTimeMillis() - startTime);
            throw new RuntimeException("Qwen embedding failed", e);
        }
    }

    @Override
    public int countTokens(String text) {
        // 粗略估算：中文约 1.5 字符/token，英文约 4 字符/token
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int otherChars = text.length() - chineseChars;
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    @Override
    public boolean isAvailable() {
        try {
            ChatRequest testRequest = ChatRequest.simple("test");
            ChatResponse response = chat(testRequest);
            return response.isSuccess();
        } catch (Exception e) {
            log.warn("Qwen provider not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ModelInfo getModelInfo() {
        return ModelInfo.builder()
                .modelName(chatModel)
                .providerType(LlmProviderType.QWEN)
                .maxContextLength(8192)
                .maxOutputTokens(maxTokens)
                .supportsStreaming(true)
                .supportsEmbedding(true)
                .embeddingDimension(1536)
                .build();
    }

    private Map<String, Object> buildChatBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", selectChatModel());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
        body.put("enable_thinking", false);

        // 构建消息列表
        List<Map<String, String>> messages = new java.util.ArrayList<>();

        // 添加系统提示
        if (request.getSystemPrompt() != null) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // 添加用户消息
        for (ChatRequest.Message msg : request.getMessages()) {
            Map<String, String> message = new HashMap<>();
            message.put("role", msg.getRole().name().toLowerCase());
            message.put("content", msg.getContent());
            messages.add(message);
        }

        body.put("messages", messages);

        // 停止序列
        if (request.getStop() != null) {
            body.put("stop", request.getStop());
        }

        return body;
    }

    private ChatResponse parseChatResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();

                JsonNode usage = root.path("usage");
                int promptTokens = usage.path("prompt_tokens").asInt();
                int completionTokens = usage.path("completion_tokens").asInt();
                int totalTokens = usage.path("total_tokens").asInt();

                return ChatResponse.builder()
                        .content(content)
                        .model(root.path("model").asText())
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .totalTokens(totalTokens)
                        .success(true)
                        .build();
            }

            return ChatResponse.failure("Empty response from Qwen");
        } catch (Exception e) {
            log.error("Failed to parse Qwen response", e);
            return ChatResponse.failure("Failed to parse response: " + e.getMessage());
        }
    }

    private List<float[]> parseEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddings = root.path("output").path("embeddings");

            List<float[]> vectors = new java.util.ArrayList<>();
            if (embeddings.isArray()) {
                for (JsonNode embedding : embeddings) {
                    float[] vector = objectMapper.convertValue(embedding, float[].class);
                    vectors.add(vector);
                }
            }

            return vectors;
        } catch (Exception e) {
            log.error("Failed to parse embedding response", e);
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }

    private List<String> parseModelPool(String rawModels) {
        if (rawModels == null || rawModels.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawModels.split(","))
                .map(String::trim)
                .filter(model -> !model.isEmpty())
                .filter(this::isAllowedChatModel)
                .distinct()
                .toList();
    }

    private Set<String> parseAllowedModelSet(String rawModels) {
        if (rawModels == null || rawModels.isBlank()) {
            return Set.of("qwen3.6-flash");
        }
        return Arrays.stream(rawModels.split(","))
                .map(String::trim)
                .filter(model -> !model.isEmpty())
                .map(model -> model.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String selectChatModel() {
        List<String> candidates = compatibleNonStreamingModels();
        if (candidates.isEmpty()) {
            return isAllowedChatModel(chatModel) ? chatModel : "qwen3.6-flash";
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private List<String> compatibleNonStreamingModels() {
        return chatModelPool.stream()
                .filter(this::supportsNonStreamingChat)
                .toList();
    }

    private boolean supportsNonStreamingChat(String model) {
        if (model == null) {
            return false;
        }
        String normalized = model.trim().toLowerCase();
        return !normalized.equals("glm-4.5-air")
                && !normalized.contains("ocr");
    }

    private boolean isAllowedChatModel(String model) {
        return model != null && allowedChatModelSet.contains(model.trim().toLowerCase(Locale.ROOT));
    }

    private void recordChatAudit(ChatRequest request, ChatResponse response, String selectedModel, long durationMs) {
        if (aiUsageAuditService == null) {
            return;
        }

        String model = response.getModel() != null && !response.getModel().isBlank()
                ? response.getModel()
                : selectedModel;
        aiUsageAuditService.record(AiUsageAuditService.AiUsageAuditEvent.builder()
                .provider("qwen")
                .model(model)
                .feature("shared-chat")
                .promptTokens(response.getPromptTokens() != null ? response.getPromptTokens() : estimateChatPromptTokens(request))
                .completionTokens(response.getCompletionTokens() != null ? response.getCompletionTokens() : 0)
                .totalTokens(response.getTotalTokens() != null ? response.getTotalTokens() : 0)
                .durationMs(durationMs)
                .success(response.isSuccess())
                .build());
    }

    private void recordChatFailure(ChatRequest request, String selectedModel, long durationMs) {
        if (aiUsageAuditService == null) {
            return;
        }

        aiUsageAuditService.record(AiUsageAuditService.AiUsageAuditEvent.builder()
                .provider("qwen")
                .model(selectedModel != null ? selectedModel : chatModel)
                .feature("shared-chat")
                .promptTokens(estimateChatPromptTokens(request))
                .durationMs(durationMs)
                .success(false)
                .build());
    }

    private void recordEmbeddingAudit(List<String> texts, String response, boolean success, long durationMs) {
        if (aiUsageAuditService == null) {
            return;
        }

        long promptTokens = estimateTextsTokens(texts);
        long totalTokens = usageToken(response, "total_tokens");
        if (totalTokens == 0) {
            totalTokens = promptTokens;
        }

        aiUsageAuditService.record(AiUsageAuditService.AiUsageAuditEvent.builder()
                .provider("qwen")
                .model(embeddingModel)
                .feature("shared-embedding")
                .promptTokens(promptTokens)
                .totalTokens(totalTokens)
                .durationMs(durationMs)
                .success(success)
                .build());
    }

    private int estimateChatPromptTokens(ChatRequest request) {
        if (request == null) {
            return 0;
        }
        int total = AiUsageAuditService.estimateTokens(request.getSystemPrompt());
        if (request.getMessages() != null) {
            for (ChatRequest.Message message : request.getMessages()) {
                total += AiUsageAuditService.estimateTokens(message.getContent());
            }
        }
        return total;
    }

    private long estimateTextsTokens(List<String> texts) {
        if (texts == null) {
            return 0;
        }
        return texts.stream()
                .mapToLong(AiUsageAuditService::estimateTokens)
                .sum();
    }

    private long usageToken(String response, String field) {
        if (response == null || response.isBlank()) {
            return 0;
        }
        try {
            return objectMapper.readTree(response).path("usage").path(field).asLong();
        } catch (Exception e) {
            return 0;
        }
    }
}
