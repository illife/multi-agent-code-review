package com.think.platform.shared.ai.llm;

import java.util.List;

/**
 * LLM 提供者接口
 * 抽象不同的 LLM 服务 (Qwen, GLM, Claude, OpenAI 等)
 *
 * @author AI Code Mentor Team
 */
public interface LlmProvider {

    /**
     * 获取提供者名称
     */
    String getName();

    /**
     * 获取提供者类型
     */
    LlmProviderType getType();

    /**
     * 文本对话 (同步)
     *
     * @param request 对话请求
     * @return 对话响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 文本对话 (异步)
     */
    default java.util.concurrent.CompletableFuture<ChatResponse> chatAsync(ChatRequest request) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> chat(request));
    }

    /**
     * 文本对话 (流式)
     */
    default java.util.stream.Stream<String> chatStream(ChatRequest request) {
        throw new UnsupportedOperationException("Streaming not supported by this provider");
    }

    /**
     * 文本嵌入 (生成向量)
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    List<float[]> embed(List<String> texts);

    /**
     * 单文本嵌入
     */
    default float[] embed(String text) {
        return embed(java.util.List.of(text)).get(0);
    }

    /**
     * 计算输入的 Token 数量
     *
     * @param text 输入文本
     * @return Token 数量
     */
    int countTokens(String text);

    /**
     * 检查提供者是否可用
     */
    boolean isAvailable();

    /**
     * 获取模型信息
     */
    ModelInfo getModelInfo();
}
