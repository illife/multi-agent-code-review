package com.think.platform.shared.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 对话响应
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 输入 Token 数
     */
    @Builder.Default
    private Integer promptTokens = 0;

    /**
     * 输出 Token 数
     */
    @Builder.Default
    private Integer completionTokens = 0;

    /**
     * 总 Token 数
     */
    @Builder.Default
    private Integer totalTokens = 0;

    /**
     * 完成原因
     */
    private String finishReason;

    /**
     * 额外信息
     */
    @Builder.Default
    private Map<String, Object> extra = new java.util.HashMap<>();

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String content) {
        return ChatResponse.builder()
                .content(content)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ChatResponse failure(String error) {
        return ChatResponse.builder()
                .error(error)
                .success(false)
                .build();
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return success;
    }
}
