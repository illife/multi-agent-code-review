package com.think.platform.shared.common.exception;

import lombok.Getter;

/**
 * 基础异常类
 * All custom exceptions should extend this class
 *
 * @author AI Code Mentor Team
 */
@Getter
public class BaseException extends RuntimeException {

    private final Integer code;

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BaseException(String message) {
        super(message);
        this.code = 500;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
