package com.think.platform.shared.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码强度验证注解
 *
 * 使用示例：
 * <pre>
 * {@code
 * public class RegisterRequest {
 *     @ValidPassword
 *     private String password;
 * }
 * }
 * </pre>
 *
 * @author Knowledge Base Team
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {

    String message() default "密码长度必须至少6个字符";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
