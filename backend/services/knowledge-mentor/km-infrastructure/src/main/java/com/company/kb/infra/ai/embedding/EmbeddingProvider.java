package com.company.kb.infra.ai.embedding;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量化提供商接口
 * 定义AI服务提供商的文本向量化能力
 */
public interface EmbeddingProvider {

    /**
     * 生成文本向量（单个）
     * @param text 输入文本
     * @return 向量数组
     * @throws Exception 生成失败时抛出异常
     */
    float[] generateEmbedding(String text) throws Exception;

    /**
     * 批量生成文本向量（优化性能）
     *
     * <p>默认实现使用循环调用单个方法，子类可以重写以支持真正的批量API调用。</p>
     *
     * @param texts 输入文本列表
     * @return 向量数组列表
     * @throws Exception 生成失败时抛出异常
     */
    default List<float[]> generateEmbeddingsBatch(List<String> texts) throws Exception {
        List<float[]> embeddings = new java.util.ArrayList<>(texts.size());
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }

    /**
     * 获取提供商名称
     * @return 提供商名称（如 qwen, glm）
     */
    String getProviderName();
}
