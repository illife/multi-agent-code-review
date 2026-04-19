package com.codereview.ai.domain.tool;

import java.util.Map;

/**
 * Tool Interface - 可被AI调用的工具
 *
 * AI Agent通过Function Calling API调用这些工具来执行具体操作
 *
 * @author Code Review AI Team
 */
public interface Tool {

    /**
     * 工具唯一标识
     * AI通过此名称调用工具
     */
    String getName();

    /**
     * 工具描述
     * AI通过描述决定何时使用此工具
     */
    String getDescription();

    /**
     * JSON Schema格式的参数定义
     * 遵循OpenAI Function Calling格式
     */
    String getParameterSchema();

    /**
     * 执行工具
     *
     * @param parameters 工具输入参数 (JSON对象转换为Map)
     * @param context 执行上下文 (reviewId, userId等)
     * @return 工具执行结果
     */
    ToolExecutionResult execute(Map<String, Object> parameters, ToolContext context);

    /**
     * 检查工具是否支持指定语言
     */
    boolean supportsLanguage(String language);

    /**
     * 工具分类
     */
    default ToolCategory getCategory() {
        return ToolCategory.CODE_ANALYSIS;
    }

    /**
     * 工具优先级（数字越小优先级越高）
     * 免费工具优先级应该更高
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否为免费工具（无API调用成本）
     */
    default boolean isFree() {
        return false;
    }

    /**
     * 工具分类枚举
     */
    enum ToolCategory {
        CODE_ANALYSIS,   // 代码分析
        SECURITY_SCAN,   // 安全扫描
        DATA_FETCH,      // 数据获取
        VALIDATION,      // 验证
        TRANSFORMATION   // 转换
    }
}
