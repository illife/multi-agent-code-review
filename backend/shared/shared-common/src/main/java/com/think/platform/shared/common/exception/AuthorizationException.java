package com.think.platform.shared.common.exception;

/**
 * 授权异常
 * Thrown when authorization fails (insufficient permissions, access denied, etc.)
 *
 * @author AI Code Mentor Team
 */
public class AuthorizationException extends BaseException {

    public AuthorizationException(String message) {
        super(403, message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(403, message, cause);
    }
}
