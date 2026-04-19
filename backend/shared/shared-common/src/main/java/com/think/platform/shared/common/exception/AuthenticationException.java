package com.think.platform.shared.common.exception;

/**
 * 认证异常
 * Thrown when authentication fails (invalid credentials, token expired, etc.)
 *
 * @author AI Code Mentor Team
 */
public class AuthenticationException extends BaseException {

    public AuthenticationException(String message) {
        super(401, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(401, message, cause);
    }
}
