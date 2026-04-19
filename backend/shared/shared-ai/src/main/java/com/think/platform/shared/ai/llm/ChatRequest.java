package com.think.platform.shared.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 对话请求
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户消息列表
     */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    /**
     * 模型名称
     */
    private String model;

    /**
     * 最大生成 Token 数
     * 默认值 7500，兼容 qwen-turbo 模型 (最大输出 8192)
     * 如需更高 token 限制，请使用 qwen-plus 或 qwen-max 模型
     */
    @Builder.Default
    private Integer maxTokens = 7500;

    /**
     * 温度参数 (0-1, 越高越随机)
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Top-P 采样参数
     */
    @Builder.Default
    private Double topP = 0.9;

    /**
     * 停止序列
     */
    private List<String> stop;

    /**
     * 额外参数
     */
    @Builder.Default
    private Map<String, Object> extraParams = new HashMap<>();

    /**
     * 添加用户消息
     */
    public ChatRequest addUserMessage(String content) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(new Message(Role.USER, content));
        return this;
    }

    /**
     * 添加助手消息
     */
    public ChatRequest addAssistantMessage(String content) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(new Message(Role.ASSISTANT, content));
        return this;
    }

    /**
     * 添加系统消息
     */
    public ChatRequest addSystemMessage(String content) {
        if (this.systemPrompt == null) {
            this.systemPrompt = content;
        } else {
            this.systemPrompt += "\n" + content;
        }
        return this;
    }

    /**
     * 消息角色
     */
    public enum Role {
        SYSTEM,    // 系统消息
        USER,      // 用户消息
        ASSISTANT  // 助手消息
    }

    /**
     * 消息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private Role role;
        private String content;
    }

    /**
     * 创建简单的用户请求
     */
    public static ChatRequest simple(String userMessage) {
        ChatRequest request = new ChatRequest();
        request.addUserMessage(userMessage);
        return request;
    }

    /**
     * 创建带系统提示的请求
     */
    public static ChatRequest withSystem(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);
        request.addUserMessage(userMessage);
        return request;
    }
}
