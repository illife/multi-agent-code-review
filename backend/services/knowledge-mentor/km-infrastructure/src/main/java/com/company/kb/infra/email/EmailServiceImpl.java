package com.company.kb.infra.email;

import com.think.platform.shared.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现
 *
 * 注意：这是一个基础实现，生产环境建议：
 * 1. 使用HTML邮件模板
 * 2. 添加邮件队列
 * 3. 实现邮件发送失败重试
 * 4. 添加邮件发送日志
 *
 * 只有在配置了邮件服务时才会启用此Bean
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@knowledgebase.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.expiration-hours:24}")
    private int expirationHours;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        try {
            String resetUrl = String.format("%s/reset-password?token=%s", frontendUrl, token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("密码重置请求");

            String text = String.format(
                "您好，\n\n" +
                "您收到此邮件是因为收到了密码重置请求。\n\n" +
                "请点击以下链接重置您的密码（链接将在%d小时内有效）：\n\n" +
                "%s\n\n" +
                "如果您没有请求密码重置，请忽略此邮件。\n\n" +
                "祝好，\n" +
                "知识库系统团队",
                expirationHours, resetUrl
            );

            message.setText(text);

            mailSender.send(message);
            log.info("密码重置邮件已发送: email={}", email);
        } catch (Exception e) {
            log.error("发送密码重置邮件失败: email={}", email, e);
            throw new RuntimeException("发送密码重置邮件失败", e);
        }
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("密码已重置");

            String text = String.format(
                "您好，\n\n" +
                "您的密码已成功重置。\n\n" +
                "如果您没有请求密码重置，请立即联系系统管理员。\n\n" +
                "祝好，\n" +
                "知识库系统团队"
            );

            message.setText(text);

            mailSender.send(message);
            log.info("密码重置确认邮件已发送: email={}", email);
        } catch (Exception e) {
            log.error("发送密码重置确认邮件失败: email={}", email, e);
            throw new RuntimeException("发送密码重置确认邮件失败", e);
        }
    }

    @Override
    public void sendPasswordChangedConfirmationEmail(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("密码已修改");

            String text = String.format(
                "您好，\n\n" +
                "您的密码已成功修改。\n\n" +
                "如果您没有修改密码，请立即联系系统管理员。\n\n" +
                "祝好，\n" +
                "知识库系统团队"
            );

            message.setText(text);

            mailSender.send(message);
            log.info("密码修改确认邮件已发送: email={}", email);
        } catch (Exception e) {
            log.error("发送密码修改确认邮件失败: email={}", email, e);
            throw new RuntimeException("发送密码修改确认邮件失败", e);
        }
    }
}
