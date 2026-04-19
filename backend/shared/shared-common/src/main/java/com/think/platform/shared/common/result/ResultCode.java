package com.think.platform.shared.common.result;

import lombok.Getter;

/**
 * 统一响应码枚举
 * Merged from codereview-common and knowledge-base-common
 *
 * @author AI Code Mentor Team
 */
@Getter
public enum ResultCode {

    // ===== 通用 HTTP 状态码 =====

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    ERROR(500, "操作失败"),

    /**
     * 参数校验错误
     */
    VALID_ERROR(400, "参数校验失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 系统内部错误
     */
    INTERNAL_ERROR(500, "系统内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ===== 用户认证相关 (1000-1999) =====

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1001, "用户不存在"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(1002, "用户已存在"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(1003, "密码错误"),

    /**
     * Token无效
     */
    TOKEN_INVALID(1004, "Token无效或已过期"),

    /**
     * 用户已锁定
     */
    USER_LOCKED(1005, "用户已被锁定"),

    /**
     * 用户未激活
     */
    USER_INACTIVE(1006, "用户未激活"),

    // ===== 代码审查相关 (2000-2999) =====

    /**
     * 代码审查中
     */
    REVIEW_PENDING(2001, "代码审查中"),

    /**
     * 代码审查处理中
     */
    REVIEW_PROCESSING(2002, "代码审查正在处理"),

    /**
     * 代码审查完成
     */
    REVIEW_COMPLETED(2003, "代码审查完成"),

    /**
     * 代码审查失败
     */
    REVIEW_FAILED(2004, "代码审查失败"),

    /**
     * 无效代码内容
     */
    INVALID_CODE_CONTENT(2005, "无效的代码内容"),

    /**
     * 不支持的编程语言
     */
    UNSUPPORTED_LANGUAGE(2006, "不支持的编程语言"),

    /**
     * 代码过大
     */
    CODE_TOO_LARGE(2007, "代码内容超过最大限制"),

    // ===== Agent 相关 (3000-3999) =====

    /**
     * Agent 不存在
     */
    AGENT_NOT_FOUND(3001, "Agent 不存在"),

    /**
     * Agent 执行失败
     */
    AGENT_EXECUTION_FAILED(3002, "Agent 执行失败"),

    /**
     * Agent 超时
     */
    AGENT_TIMEOUT(3003, "Agent 执行超时"),

    /**
     * Agent 编排失败
     */
    AGENT_ORCHESTRATION_FAILED(3004, "Agent 编排失败"),

    // ===== 文档相关 (4000-4999) =====

    /**
     * 文档不存在
     */
    DOCUMENT_NOT_FOUND(4001, "文档不存在"),

    /**
     * 文档解析失败
     */
    DOCUMENT_PARSE_FAILED(4002, "文档解析失败"),

    /**
     * 文档索引失败
     */
    DOCUMENT_INDEX_FAILED(4003, "文档索引失败"),

    /**
     * 不支持的文件类型
     */
    UNSUPPORTED_FILE_TYPE(4004, "不支持的文件类型"),

    /**
     * 无权访问文档
     */
    DOCUMENT_ACCESS_DENIED(4005, "无权访问该文档"),

    // ===== 搜索与 AI 相关 (5000-5999) =====

    /**
     * 搜索失败
     */
    SEARCH_FAILED(5001, "搜索失败"),

    /**
     * AI服务异常
     */
    AI_SERVICE_ERROR(5002, "AI服务异常"),

    /**
     * AI 调用超时
     */
    AI_TIMEOUT(5003, "AI 调用超时"),

    /**
     * RAG 检索失败
     */
    RAG_RETRIEVAL_FAILED(5004, "RAG 检索失败"),

    // ===== 权限相关 (6000-6999) =====

    /**
     * 权限不足
     */
    PERMISSION_DENIED(6001, "权限不足"),

    /**
     * 角色不存在
     */
    ROLE_NOT_FOUND(6002, "角色不存在"),

    // ===== 教学系统相关 (7000-7999) =====

    /**
     * 课程不存在
     */
    LESSON_NOT_FOUND(7001, "课程不存在"),

    /**
     * 练习不存在
     */
    EXERCISE_NOT_FOUND(7002, "练习不存在"),

    /**
     * 练习未通过
     */
    EXERCISE_FAILED(7003, "练习未通过");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
