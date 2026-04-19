package com.company.kb.infra.ai.chat;

/**
 * 对话提供商接口
 * 定义AI服务提供商的对话生成能力
 */
public interface ChatProvider {

    /**
     * 生成答案（非流式）
     * @param question 用户问题
     * @param context 上下文信息
     * @return 生成的答案
     * @throws Exception 生成失败时抛出异常
     */
    String generateAnswer(String question, String context) throws Exception;

    /**
     * 流式生成答案
     * @param question 用户问题
     * @param context 上下文信息
     * @param callback 流式回调接口
     * @throws Exception 生成失败时抛出异常
     */
    void streamAnswer(String question, String context, StreamCallback callback) throws Exception;

    /**
     * 获取提供商名称
     * @return 提供商名称（如 qwen, glm）
     */
    String getProviderName();

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Throwable error);
    }
}
