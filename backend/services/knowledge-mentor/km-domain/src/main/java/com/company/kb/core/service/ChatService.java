package com.company.kb.core.service;

/**
 * 对话生成服务
 * 负责基于上下文生成答案，支持非流式和流式输出
 */
public interface ChatService {

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
     * 流式回调接口
     */
    interface StreamCallback {
        /**
         * 接收生成的token
         * @param token 文本片段
         */
        void onToken(String token);

        /**
         * 生成完成
         */
        void onComplete();

        /**
         * 生成出错
         * @param error 错误信息
         */
        void onError(Throwable error);
    }
}
