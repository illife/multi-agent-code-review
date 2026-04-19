package com.think.platform.shared.common.exception;

/**
 * 业务异常
 * General business exception for application-level errors
 *
 * @author AI Code Mentor Team
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(500, message);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(500, message, cause);
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
