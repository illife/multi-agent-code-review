package com.think.platform.shared.common.exception;

/**
 * 资源不存在异常
 * Thrown when a requested resource is not found
 *
 * @author AI Code Mentor Team
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(404, String.format("%s not found with id: %d", resource, id));
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(404, String.format("%s not found: %s", resource, identifier));
    }
}
