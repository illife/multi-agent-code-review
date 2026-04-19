package com.company.kb.infra.ai.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

    @Override
    public float[] generateEmbedding(String text) throws Exception {
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

        log.debug("智谱向量化成功: dim={}", result.length);
        return result;
    }

    @Override
    public String getProviderName() {
        return "glm";
    }
}
