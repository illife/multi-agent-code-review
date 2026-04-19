package com.think.platform.shared.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码重置请求DTO
 *
 * @author Knowledge Base Team
 */
@Data
public class PasswordResetRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
