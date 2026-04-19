package com.think.platform.shared.common.exception;

/**
 * 账户被锁定异常
 *
 * @author Knowledge Base Team
 */
public class AccountLockedException extends RuntimeException {

    private final long lockTimeRemaining;

    public AccountLockedException(String message, long lockTimeRemaining) {
        super(message);
        this.lockTimeRemaining = lockTimeRemaining;
    }

    public long getLockTimeRemaining() {
        return lockTimeRemaining;
    }
}
