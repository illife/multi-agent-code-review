package com.think.platform.shared.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 请求
 * 封装传递给 Agent 的输入数据
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * Agent 类型
     */
    private AgentType agentType;

    /**
     * 输入数据
     */
    @Builder.Default
    private Map<String, Object> inputData = new HashMap<>();

    /**
     * 用户 ID (用于个性化)
     */
    private Long userId;

    /**
     * 请求来源
     */
    private String source;

    /**
     * 超时时间 (毫秒)
     */
    @Builder.Default
    private Long timeout = 30000L;

    /**
     * 是否异步执行
     */
    @Builder.Default
    private Boolean async = false;

    // ===== 便捷方法 =====

    /**
     * 添加输入数据
     */
    public AgentRequest addInput(String key, Object value) {
        if (this.inputData == null) {
            this.inputData = new HashMap<>();
        }
        this.inputData.put(key, value);
        return this;
    }

    /**
     * 获取输入数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key) {
        if (inputData == null) {
            return null;
        }
        return (T) inputData.get(key);
    }

    /**
     * 获取输入数据，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key, T defaultValue) {
        if (inputData == null || !inputData.containsKey(key)) {
            return defaultValue;
        }
        return (T) inputData.get(key);
    }

    /**
     * 创建代码审查请求
     */
    public static AgentRequest forCodeReview(String requestId, String code, String language) {
        AgentRequest request = AgentRequest.builder()
                .requestId(requestId)
                .agentType(AgentType.CODE_STANDARDS_INSPECTOR)
                .build();
        request.addInput("code", code);
        request.addInput("language", language);
        return request;
    }

    /**
     * 创建教学请求
     */
    public static AgentRequest forTeaching(String requestId, Long userId, String topic, String userLevel) {
        AgentRequest request = AgentRequest.builder()
                .requestId(requestId)
                .agentType(AgentType.TEACHING_MENTOR)
                .userId(userId)
                .build();
        request.addInput("topic", topic);
        request.addInput("userLevel", userLevel);
        return request;
    }

    /**
     * 创建练习请求
     */
    public static AgentRequest forExercise(String requestId, Long userId, String language, String difficulty) {
        AgentRequest request = AgentRequest.builder()
                .requestId(requestId)
                .agentType(AgentType.EXERCISE_COACH)
                .userId(userId)
                .build();
        request.addInput("language", language);
        request.addInput("difficulty", difficulty);
        return request;
    }
}
