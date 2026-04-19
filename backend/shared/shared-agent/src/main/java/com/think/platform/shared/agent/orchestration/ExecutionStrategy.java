package com.think.platform.shared.agent.orchestration;

/**
 * Agent 执行策略
 * 定义多个 Agent 的执行方式
 *
 * @author AI Code Mentor Team
 */
public enum ExecutionStrategy {

    /**
     * 串行执行
     * 按顺序依次执行每个 Agent，前一个完成后才开始下一个
     */
    SEQUENTIAL("sequential", "串行执行"),

    /**
     * 并行执行
     * 所有 Agent 同时执行，等待所有完成
     */
    PARALLEL("parallel", "并行执行"),

    /**
     * 流水线执行
     * 前一个 Agent 的输出作为下一个 Agent 的输入
     */
    PIPELINE("pipeline", "流水线执行"),

    /**
     * 竞争执行
     * 所有 Agent 同时执行，取第一个成功的结果
     */
    RACE("race", "竞争执行"),

    /**
     * 多数投票
     * 所有 Agent 同时执行，根据多数规则决定最终结果
     */
    MAJORITY_VOTE("majority_vote", "多数投票");

    private final String code;
    private final String description;

    ExecutionStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取 ExecutionStrategy
     */
    public static ExecutionStrategy fromCode(String code) {
        for (ExecutionStrategy strategy : values()) {
            if (strategy.code.equals(code)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown execution strategy: " + code);
    }
}
