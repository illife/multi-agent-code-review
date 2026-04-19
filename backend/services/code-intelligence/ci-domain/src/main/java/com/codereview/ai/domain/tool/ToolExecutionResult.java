package com.codereview.ai.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool Execution Result
 *
 * 工具执行后的返回结果
 *
 * @author Code Review AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 执行结果数据
     * 可以是List、Map或其他结构化数据
     */
    private Object data;

    /**
     * 错误信息（如果执行失败）
     */
    private String error;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;

    /**
     * 输出格式
     */
    @Builder.Default
    private String outputFormat = "json";

    /**
     * 创建失败结果
     */
    public static ToolExecutionResult failure(String error) {
        return ToolExecutionResult.builder()
                .success(false)
                .error(error)
                .build();
    }

    /**
     * 创建成功结果
     */
    public static ToolExecutionResult success(Object data) {
        return ToolExecutionResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建成功结果（带执行时间）
     */
    public static ToolExecutionResult success(Object data, long executionTimeMs) {
        return ToolExecutionResult.builder()
                .success(true)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .build();
    }
}
