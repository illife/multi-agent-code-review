package com.codereview.ai.domain.agent.shared;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 执行结果
 * 封装 Agent 的执行结果
 *
 * @author AI Code Mentor Team
 */
@Data
@Builder
public class AgentExecutionResult {

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = false;

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
     * 发现的问题列表
     */
    @Builder.Default
    private List<AgentIssue> issues = new ArrayList<>();

    /**
     * 错误信息
     */
    private String error;

    /**
     * 执行耗时 (毫秒)
     */
    @Builder.Default
    private long executionTimeMs = 0;

    /**
     * Token 使用统计
     */
    private TokenUsage tokenUsage;

    /**
     * 添加输出数据
     */
    public AgentExecutionResult addOutput(String key, Object value) {
        if (this.outputData == null) {
            this.outputData = new HashMap<>();
        }
        this.outputData.put(key, value);
        return this;
    }

    /**
     * 添加问题
     */
    public AgentExecutionResult addIssue(AgentIssue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
        return this;
    }

    /**
     * 获取问题数量
     */
    public int getIssueCount() {
        return issues != null ? issues.size() : 0;
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
     * 创建失败结果
     */
    public static AgentExecutionResult failure(String agentType, String error) {
        return AgentExecutionResult.builder()
                .agentType(agentType)
                .success(false)
                .error(error)
                .build();
    }

    /**
     * Token 使用统计
     */
    @Data
    @Builder
    public static class TokenUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    /**
     * Agent 发现的问题
     */
    @Data
    @Builder
    public static class AgentIssue {
        /**
         * 问题标题
         */
        private String title;

        /**
         * 问题描述
         */
        private String description;

        /**
         * 严重程度
         */
        private Severity severity;

        /**
         * 类别
         */
        private String category;

        /**
         * 行号
         */
        private Integer lineNumber;

        /**
         * 代码片段
         */
        private String codeSnippet;

        /**
         * 修复建议
         */
        private String suggestion;

        /**
         * 教学解释
         */
        private String teachingExplanation;

        /**
         * Agent 类型
         */
        private String agentType;

        /**
         * 相关概念
         */
        private List<String> relatedConcepts;

        /**
         * 置信度 (0-1)
         */
        private double confidence;

        /**
         * 获取 Agent 类型
         */
        public String getAgentType() {
            return agentType;
        }
    }

    /**
     * 问题严重程度
     */
    public enum Severity {
        CRITICAL,  // 严重 - 必须修复
        HIGH,      // 高 - 应该修复
        MEDIUM,    // 中 - 建议修复
        LOW,       // 低 - 可选修复
        INFO       // 信息 - 仅提示
    }
}
