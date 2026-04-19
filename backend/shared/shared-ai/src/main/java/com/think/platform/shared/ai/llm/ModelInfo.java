package com.think.platform.shared.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 模型信息
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 提供者类型
     */
    private LlmProviderType providerType;

    /**
     * 最大上下文长度 (Token 数)
     */
    @Builder.Default
    private Integer maxContextLength = 4096;

    /**
     * 最大输出 Token 数
     */
    @Builder.Default
    private Integer maxOutputTokens = 2000;

    /**
     * 是否支持函数调用
     */
    @Builder.Default
    private boolean supportsFunctionCalling = false;

    /**
     * 是否支持流式输出
     */
    @Builder.Default
    private boolean supportsStreaming = true;

    /**
     * 是否支持嵌入
     */
    @Builder.Default
    private boolean supportsEmbedding = false;

    /**
     * 输入价格 (每 1K Token)
     */
    @Builder.Default
    private Double inputPricePer1k = 0.0;

    /**
     * 输出价格 (每 1K Token)
     */
    @Builder.Default
    private Double outputPricePer1k = 0.0;

    /**
     * 嵌入向量维度
     */
    @Builder.Default
    private Integer embeddingDimension = 1536;
}
