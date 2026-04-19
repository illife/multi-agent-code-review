package com.think.platform.shared.agent.core;

import java.util.concurrent.CompletableFuture;

/**
 * Agent 接口
 * 所有 Agent 必须实现此接口
 *
 * @author AI Code Mentor Team
 */
public interface Agent {

    /**
     * 获取 Agent 类型
     */
    AgentType getAgentType();

    /**
     * 获取 Agent 名称
     */
    String getName();

    /**
     * 获取 Agent 描述
     */
    String getDescription();

    /**
     * 执行 Agent 逻辑 (同步)
     *
     * @param request Agent 请求
     * @return Agent 结果
     */
    AgentResult execute(AgentRequest request);

    /**
     * 执行 Agent 逻辑 (异步)
     *
     * @param request Agent 请求
     * @return CompletableFuture 包装的 Agent 结果
     */
    default CompletableFuture<AgentResult> executeAsync(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> execute(request));
    }

    /**
     * 检查是否支持给定的请求类型
     *
     * @param agentType Agent 类型
     * @return 是否支持
     */
    default boolean supports(AgentType agentType) {
        return this.getAgentType() == agentType;
    }

    /**
     * 检查 Agent 是否可用
     *
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取 Agent 优先级 (数字越小优先级越高)
     *
     * @return 优先级
     */
    default int getPriority() {
        return getAgentType().getPriority();
    }
}
