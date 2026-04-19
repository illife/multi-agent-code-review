package com.think.platform.shared.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 执行结果
 * 封装 Agent 的执行结果数据
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * Agent 类型
     */
    private AgentType agentType;

    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = false;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 输出数据
     */
    @Builder.Default
    private Map<String, Object> outputData = new HashMap<>();

    /**
     * 错误信息
     */
    private String error;

    /**
     * 错误堆栈
     */
    private String stackTrace;

    /**
     * 执行耗时 (毫秒)
     */
    @Builder.Default
    private Long executionTime = 0L;

    /**
     * 执行时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ===== 静态工厂方法 =====

    /**
     * 创建成功结果
     */
    public static AgentResult success(String requestId, AgentType agentType, String message) {
        return AgentResult.builder()
                .requestId(requestId)
                .agentType(agentType)
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 创建成功结果 (带数据)
     */
    public static AgentResult success(String requestId, AgentType agentType, String message, Map<String, Object> data) {
        return AgentResult.builder()
                .requestId(requestId)
                .agentType(agentType)
                .success(true)
                .message(message)
                .outputData(data)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static AgentResult failure(String requestId, AgentType agentType, String error) {
        return AgentResult.builder()
                .requestId(requestId)
                .agentType(agentType)
                .success(false)
                .error(error)
                .build();
    }

    /**
     * 创建失败结果 (带异常)
     */
    public static AgentResult failure(String requestId, AgentType agentType, String error, Throwable throwable) {
        return AgentResult.builder()
                .requestId(requestId)
                .agentType(agentType)
                .success(false)
                .error(error)
                .stackTrace(throwable != null ? getStackTrace(throwable) : null)
                .build();
    }

    // ===== 便捷方法 =====

    /**
     * 添加输出数据
     */
    public AgentResult addOutput(String key, Object value) {
        if (this.outputData == null) {
            this.outputData = new HashMap<>();
        }
        this.outputData.put(key, value);
        return this;
    }

    /**
     * 获取输出数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput(String key) {
        if (outputData == null) {
            return null;
        }
        return (T) outputData.get(key);
    }

    /**
     * 获取输出数据，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput(String key, T defaultValue) {
        if (outputData == null || !outputData.containsKey(key)) {
            return defaultValue;
        }
        return (T) outputData.get(key);
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 获取异常堆栈
     */
    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
