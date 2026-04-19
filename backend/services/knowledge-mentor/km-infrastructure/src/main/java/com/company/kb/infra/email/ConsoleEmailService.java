package com.company.kb.infra.email;

import com.think.platform.shared.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 控制台邮件服务（开发环境）
 *
 * 当没有配置真实邮件服务时，使用此服务在控制台输出邮件内容
 * 开发人员可以从日志中复制验证链接进行测试
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Service
@ConditionalOnMissingBean(EmailServiceImpl.class)
public class ConsoleEmailService implements EmailService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.expiration-hours:24}")
    private int expirationHours;

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = String.format("%s/reset-password?token=%s", frontendUrl, token);

        log.info("====================================");
        log.info("📧 [开发环境] 密码重置邮件");
        log.info("====================================");
        log.info("收件人: {}", email);
        log.info("重置链接: {}", resetUrl);
        log.info("Token: {}", token);
        log.info("有效期: {} 小时", expirationHours);
        log.info("====================================");
        log.info("请在浏览器中打开上述链接进行密码重置");
        log.info("====================================");
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String email) {
        log.info("====================================");
        log.info("📧 [开发环境] 密码已重置通知");
        log.info("====================================");
        log.info("收件人: {}", email);
        log.info("您的密码已成功重置");
        log.info("如果您没有请求密码重置，请立即联系系统管理员");
        log.info("====================================");
    }

    @Override
    public void sendPasswordChangedConfirmationEmail(String email) {
        log.info("====================================");
        log.info("📧 [开发环境] 密码已修改通知");
        log.info("====================================");
        log.info("收件人: {}", email);
        log.info("您的密码已成功修改");
        log.info("如果您没有修改密码，请立即联系系统管理员");
        log.info("====================================");
    }
}
