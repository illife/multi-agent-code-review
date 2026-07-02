package com.think.platform.shared.infra.ai;

/**
 * Raised when a user exceeds configured AI usage limits.
 */
public class AiUsageLimitExceededException extends RuntimeException {

    public AiUsageLimitExceededException(String message) {
        super(message);
    }

    public static boolean causedBy(Throwable throwable) {
        return find(throwable) != null;
    }

    public static AiUsageLimitExceededException find(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AiUsageLimitExceededException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }
}
