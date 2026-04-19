package com.company.kb.infra.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 千问文本向量化提供商
 * 实现阿里云通义千问的文本向量化功能
 */
@Slf4j
@Component
@org.springframework.context.annotation.Primary
public class QwenEmbeddingProvider implements EmbeddingProvider {

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.api-url}")
    private String apiUrl;

    @Value("${qwen.embedding-model}")
    private String embeddingModel;

    private final WebClient.Builder webClientBuilder = WebClient.builder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public float[] generateEmbedding(String text) throws Exception {
        log.debug("千问向量化: textLength={}", text.length());

        try {
            // OpenAI兼容格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", text);  // 单个文本直接传入

            String response = webClientBuilder.build()
                .post()
                .uri(apiUrl + "/embeddings")  // OpenAI兼容端点
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isArray() && data.size() > 0) {
                JsonNode embedding = data.get(0).path("embedding");
                List<Float> vectorList = new ArrayList<>();
                for (JsonNode value : embedding) {
                    vectorList.add((float) value.asDouble());
                }

                float[] vector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vector[i] = vectorList.get(i);
                }

                log.debug("千问向量化成功: dim={}", vector.length);
                return vector;
            } else {
                throw new RuntimeException("向量化失败：响应格式错误，data=" + data.toString());
            }

        } catch (Exception e) {
            log.error("千问向量化失败: textLength={}", text.length(), e);
            throw new Exception("向量化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    /**
     * 批量向量化实现（优化性能）
     *
     * 使用OpenAI兼容格式，一次性处理多个文本，避免多次网络请求
     *
     * Qwen API限制：批量请求最多10个文本（避免400错误）
     *
     * @param texts 输入文本列表
     * @return 向量数组列表
     * @throws Exception 生成失败时抛出异常
     */
    @Override
    public List<float[]> generateEmbeddingsBatch(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        // 如果只有一个文本，直接调用单个方法
        if (texts.size() == 1) {
            List<float[]> result = new ArrayList<>();
            result.add(generateEmbedding(texts.get(0)));
            return result;
        }

        log.info("千问批量向量化: textCount={}", texts.size());

        // Qwen API批量限制：每次最多10个文本
        final int BATCH_SIZE = 10;
        List<float[]> allResults = new ArrayList<>();

        // 分批处理
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            log.info("处理批次: [{}/{}, size={}]", i, texts.size(), batch.size());

            try {
                List<float[]> batchResults = processSingleBatch(batch);
                allResults.addAll(batchResults);
            } catch (Exception e) {
                log.error("批次处理失败: [{}/{}], 降级为逐个处理", i, end);
                // 降级为逐个处理
                for (String text : batch) {
                    try {
                        allResults.add(generateEmbedding(text));
                    } catch (Exception ex) {
                        log.error("单个文本向量化失败: textLength={}", text.length(), ex);
                        throw new Exception("文本向量化失败: " + ex.getMessage(), ex);
                    }
                }
            }
        }

        log.info("千问批量向量化完成: total={}", allResults.size());
        return allResults;
    }

    /**
     * 处理单个批次
     */
    private List<float[]> processSingleBatch(List<String> texts) throws Exception {
        try {
            // OpenAI兼容格式：input是字符串数组
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", texts);  // 直接传入List<String>

            long startTime = System.currentTimeMillis();

            // 发送批量请求
            String response = webClientBuilder.build()
                .post()
                .uri(apiUrl + "/embeddings")  // OpenAI兼容端点
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            long duration = System.currentTimeMillis() - startTime;

            // 解析响应：OpenAI格式
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                throw new RuntimeException("批量向量化失败：响应格式错误，期望data数组，实际=" + root.toString());
            }

            // 提取所有向量（按index排序，确保顺序正确）
            List<float[]> result = new ArrayList<>(data.size());

            // OpenAI返回的data可能不按顺序，需要按index排序
            List<JsonNode> sortedData = new ArrayList<>();
            for (JsonNode item : data) {
                sortedData.add(item);
            }
            sortedData.sort((a, b) -> a.path("index").asInt() - b.path("index").asInt());

            for (JsonNode item : sortedData) {
                JsonNode embedding = item.path("embedding");
                List<Float> vectorList = new ArrayList<>();
                for (JsonNode value : embedding) {
                    vectorList.add((float) value.asDouble());
                }

                float[] vector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vector[i] = vectorList.get(i);
                }
                result.add(vector);
            }

            log.info("千问批次向量化成功: count={}, duration={}ms, avgPerText={}ms",
                result.size(), duration, duration / result.size());

            return result;

        } catch (Exception e) {
            log.error("千问批次向量化失败: textCount={}", texts.size(), e);
            throw e;
        }
    }
}
