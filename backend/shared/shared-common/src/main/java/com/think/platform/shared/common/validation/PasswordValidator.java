package com.think.platform.shared.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 密码强度验证器
 * 密码必须满足：
 * 1. 至少6个字符
 * 2. 包含字母或数字
 *
 * @author Knowledge Base Team
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // 密码长度：至少6个字符
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 100;

    // 正则表达式模式
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        // 检查长度
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }

        // 检查是否包含字母或数字（至少满足一个）
        boolean hasLetter = LETTER_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();

        if (!hasLetter && !hasDigit) {
            return false;
        }

        return true;
    }
}
