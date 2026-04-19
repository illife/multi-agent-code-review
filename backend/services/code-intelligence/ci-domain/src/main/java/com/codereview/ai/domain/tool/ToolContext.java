package com.codereview.ai.domain.tool;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Tool Execution Context
 *
 * 工具执行时的上下文信息
 *
 * @author Code Review AI Team
 */
@Data
@Builder
public class ToolContext {

    /**
     * 代码审查ID
     */
    private Long reviewId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 代码内容
     */
    private String code;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;
}
