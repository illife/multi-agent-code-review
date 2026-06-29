package com.codereview.auth.infrastructure.email;

import com.think.platform.shared.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Auth transactional email service backed by Resend.
 */
@Slf4j
@Service
public class AuthEmailService implements EmailService {

    private final RestClient restClient;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:CodeView <noreply@send.codeview.top>}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.expiration-hours:24}")
    private int passwordResetExpirationHours;

    @Value("${app.email-verification.expiration-hours:48}")
    private int emailVerificationExpirationHours;

    public AuthEmailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .defaultHeader("User-Agent", "CodeView-Auth-Service/1.0")
                .build();
    }

    @Override
    public void sendEmailVerificationEmail(String email, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + encode(token);
        String html = renderEmail(
                "确认你的 CodeView 邮箱",
                "完成邮箱确认",
                "感谢注册 CodeView。请确认这个邮箱归你所有，后续找回密码和账号安全提醒都会发送到这里。",
                verifyUrl,
                "确认邮箱",
                "该链接将在 " + emailVerificationExpirationHours + " 小时后失效。如果这不是你本人操作，可以忽略这封邮件。"
        );

        send(email, "确认你的 CodeView 邮箱", html, "邮箱验证链接: " + verifyUrl);
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + encode(token);
        String html = renderEmail(
                "重置你的 CodeView 密码",
                "收到密码重置请求",
                "有人请求重置你的 CodeView 账号密码。如果这是你本人操作，请点击下面的按钮设置新密码。",
                resetUrl,
                "重置密码",
                "该链接将在 " + passwordResetExpirationHours + " 小时后失效。如果你没有请求重置密码，请忽略这封邮件。"
        );

        send(email, "重置你的 CodeView 密码", html, "密码重置链接: " + resetUrl);
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String email) {
        String html = renderNoticeEmail(
                "CodeView 密码已重置",
                "你的密码已经成功重置。",
                "如果这不是你本人操作，请尽快再次修改密码，或联系站点管理员检查账号安全。"
        );

        send(email, "CodeView 密码已重置", html, "你的密码已经成功重置。");
    }

    @Override
    public void sendPasswordChangedConfirmationEmail(String email) {
        String html = renderNoticeEmail(
                "CodeView 密码已修改",
                "你的密码已经成功修改。",
                "如果这不是你本人操作，请立即使用忘记密码流程重置密码。"
        );

        send(email, "CodeView 密码已修改", html, "你的密码已经成功修改。");
    }

    private void send(String to, String subject, String html, String fallbackText) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.info("Email delivery skipped because RESEND_API_KEY is not configured. to={}, subject={}, {}", to, subject, fallbackText);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "html", html
        );

        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + resendApiKey)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Transactional email sent via Resend: to={}, subject={}", to, subject);
    }

    private String renderEmail(String title, String heading, String body, String actionUrl, String actionLabel, String footer) {
        String escapedTitle = escape(title);
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#f6f3ec;font-family:Arial,'Microsoft YaHei',sans-serif;color:#0f172a;">
                    <div style="max-width:560px;margin:0 auto;padding:32px 18px;">
                      <div style="background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:28px;">
                        <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#0f766e;font-weight:700;">CodeView</div>
                        <h1 style="margin:14px 0 10px;font-size:24px;line-height:1.25;color:#0f172a;">%s</h1>
                        <p style="margin:0 0 24px;font-size:15px;line-height:1.8;color:#475569;">%s</p>
                        <a href="%s" style="display:inline-block;background:#0f172a;color:#ffffff;text-decoration:none;border-radius:8px;padding:12px 18px;font-weight:700;">%s</a>
                        <p style="margin:24px 0 0;font-size:13px;line-height:1.7;color:#64748b;">%s</p>
                      </div>
                      <p style="margin:16px 0 0;font-size:12px;line-height:1.6;color:#64748b;">如果按钮无法打开，请复制链接到浏览器：<br><span style="word-break:break-all;">%s</span></p>
                    </div>
                  </body>
                </html>
                """.formatted(
                escape(heading),
                escape(body),
                escape(actionUrl),
                escape(actionLabel),
                escape(footer),
                escape(actionUrl)
        ).replace("<html>", "<html><head><title>" + escapedTitle + "</title></head>");
    }

    private String renderNoticeEmail(String title, String body, String footer) {
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#f6f3ec;font-family:Arial,'Microsoft YaHei',sans-serif;color:#0f172a;">
                    <div style="max-width:560px;margin:0 auto;padding:32px 18px;">
                      <div style="background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:28px;">
                        <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#0f766e;font-weight:700;">CodeView</div>
                        <h1 style="margin:14px 0 10px;font-size:24px;line-height:1.25;color:#0f172a;">%s</h1>
                        <p style="margin:0;font-size:15px;line-height:1.8;color:#475569;">%s</p>
                        <p style="margin:22px 0 0;font-size:13px;line-height:1.7;color:#64748b;">%s</p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(escape(title), escape(body), escape(footer));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
