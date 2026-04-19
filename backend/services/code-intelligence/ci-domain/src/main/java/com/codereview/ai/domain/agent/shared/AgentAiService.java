package com.codereview.ai.domain.agent.shared;

/**
 * Agent AI 服务接口
 * 简化 AI 调用的统一接口
 *
 * @author AI Code Mentor Team
 */
public interface AgentAiService {

    /**
     * 文本对话
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt    用户提示词
     * @return AI 响应内容
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 文本对话 (带上下文)
     *
     * @param systemPrompt  系统提示词
     * @param userPrompt     用户提示词
     * @param conversationId 会话 ID (用于上下文记忆)
     * @return AI 响应内容
     */
    String chatWithContext(String systemPrompt, String userPrompt, String conversationId);

    /**
     * 生成文本嵌入
     *
     * @param text 文本
     * @return 嵌入向量
     */
    float[] embed(String text);
}
