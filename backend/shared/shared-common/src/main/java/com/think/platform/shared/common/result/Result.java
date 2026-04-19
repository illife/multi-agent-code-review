package com.think.platform.shared.common.result;

import lombok.Data;

/**
 * 统一响应结果封装
 * Merged from codereview-common and knowledge-base-common
 *
 * @param <T> 数据类型
 * @author AI Code Mentor Team
 */
@Data
public class Result<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ===== 成功响应 =====

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ===== 失败响应 =====

    /**
     * 失败响应（默认错误码）
     */
    public static <T> Result<T> failed() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> failed(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 失败响应（指定错误码）
     */
    public static <T> Result<T> failed(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（指定错误码和消息）
     */
    public static <T> Result<T> failed(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * 失败响应（指定码和消息）
     */
    public static <T> Result<T> failed(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // ===== 便捷方法 =====

    /**
     * 错误响应（与 failed 同义，保持向后兼容）
     */
    public static <T> Result<T> error(String message) {
        return failed(message);
    }

    /**
     * 错误响应（指定码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return failed(code, message);
    }

    /**
     * 错误响应（指定错误码）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return failed(resultCode);
    }

    // ===== HTTP 状态码便捷方法 =====

    /**
     * 400 错误
     */
    public static <T> Result<T> badRequest(String message) {
        return failed(ResultCode.VALID_ERROR.getCode(), message);
    }

    /**
     * 401 未授权
     */
    public static <T> Result<T> unauthorized(String message) {
        return failed(ResultCode.UNAUTHORIZED.getCode(), message);
    }

    /**
     * 403 禁止访问
     */
    public static <T> Result<T> forbidden(String message) {
        return failed(ResultCode.FORBIDDEN.getCode(), message);
    }

    /**
     * 404 资源不存在
     */
    public static <T> Result<T> notFound(String message) {
        return failed(ResultCode.NOT_FOUND.getCode(), message);
    }
}
