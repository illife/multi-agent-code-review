package com.think.platform.shared.common.service;

/**
 * Email Service Interface
 * 统一的邮件服务接口
 *
 * @author AI Code Mentor Team
 */
public interface EmailService {

    /**
     * 发送密码重置邮件
     *
     * @param email 收件人邮箱
     * @param token 重置令牌
     */
    void sendPasswordResetEmail(String email, String token);

    /**
     * 发送密码重置确认邮件
     *
     * @param email 收件人邮箱
     */
    void sendPasswordResetConfirmationEmail(String email);

    /**
     * 发送密码修改确认邮件
     *
     * @param email 收件人邮箱
     */
    void sendPasswordChangedConfirmationEmail(String email);
}
