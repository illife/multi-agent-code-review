package com.company.kb.infra.ai.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.infra.ai.AiUsageAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 智谱文本向量化提供商
 * 实现智谱AI的文本向量化功能
 */
@Slf4j
@Component
public class GLMEmbeddingProvider implements EmbeddingProvider {

    @Value("${glm.api-url}")
    private String apiUrl;

    @Value("${glm.api-key}")
    private String apiKey;

    @Value("${glm.embedding-model:embedding-2}")
    private String embeddingModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Nullable
    private final AiUsageAuditService aiUsageAuditService;

    public GLMEmbeddingProvider(@Nullable AiUsageAuditService aiUsageAuditService) {
        this.aiUsageAuditService = aiUsageAuditService;
    }

    @Override
    public float[] generateEmbedding(String text) throws Exception {
        long startTime = System.currentTimeMillis();
        log.debug("智谱向量化: textLength={}", text.length());

        String url = apiUrl + "/embeddings";

        // 构建请求体
        String requestBody = String.format(
            "{\"model\":\"%s\",\"input\":[\"%s\"],\"dimensions\":1024}",
            embeddingModel,
            text.replace("\"", "\\\"").replace("\n", "\\n")
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
            JsonNode embedding = root.path("data").get(0).path("embedding");

            // 转换为数组
            List<Double> embeddings = objectMapper.convertValue(embedding,
                new TypeReference<List<Double>>() {});

            float[] result = new float[embeddings.size()];
            for (int i = 0; i < embeddings.size(); i++) {
                result[i] = embeddings.get(i).floatValue();
            }

            recordAudit(text, root, System.currentTimeMillis() - startTime, true);
            log.debug("智谱向量化成功: dim={}", result.length);
            return result;
        } catch (Exception e) {
            recordAudit(text, null, System.currentTimeMillis() - startTime, false);
            throw e;
        }
    }

    @Override
    public String getProviderName() {
        return "glm";
    }

    private void recordAudit(String text, JsonNode responseRoot, long durationMs, boolean success) {
        if (aiUsageAuditService == null) {
            return;
        }

        long promptTokens = AiUsageAuditService.estimateTokens(text);
        JsonNode usage = responseRoot != null ? responseRoot.path("usage") : null;
        long totalTokens = usage != null ? usage.path("total_tokens").asLong(0) : 0;
        if (totalTokens == 0) {
            totalTokens = promptTokens;
        }

        aiUsageAuditService.record(AiUsageAuditService.AiUsageAuditEvent.builder()
            .provider("glm")
            .model(embeddingModel)
            .feature("knowledge-embedding")
            .promptTokens(promptTokens)
            .totalTokens(totalTokens)
            .durationMs(durationMs)
            .success(success)
            .build());
    }
}
