package com.think.platform.shared.common.dto;

import com.think.platform.shared.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码重置确认请求DTO
 *
 * @author Knowledge Base Team
 */
@Data
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Token不能为空")
    private String token;

    @NotBlank(message = "密码不能为空")
    @ValidPassword
    private String newPassword;
}
