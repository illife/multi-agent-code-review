package com.codereview.ai.domain.agent.shared;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Agent 执行上下文
 * 封装 Agent 执行所需的所有信息
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
public class AgentExecutionContext {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 代码内容
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 文件路径 (可选)
     */
    private String filePath;

    /**
     * 项目 ID (可选)
     */
    private Long projectId;

    /**
     * 额外的上下文数据
     */
    @Builder.Default
    private Map<String, Object> contextData = Map.of();

    /**
     * AI 服务
     */
    private transient AgentAiService aiService;

    /**
     * 获取上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key) {
        if (contextData == null) {
            return null;
        }
        return (T) contextData.get(key);
    }

    /**
     * 获取上下文数据，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key, T defaultValue) {
        if (contextData == null || !contextData.containsKey(key)) {
            return defaultValue;
        }
        return (T) contextData.get(key);
    }

    /**
     * 添加上下文数据
     */
    public void addContextData(String key, Object value) {
        if (contextData == null) {
            contextData = Map.of();
        }
        // 由于 Map.of() 返回不可变 Map，需要创建新的可变 Map
        Map<String, Object> newMap = new java.util.HashMap<>(contextData);
        newMap.put(key, value);
        this.contextData = newMap;
    }
}
