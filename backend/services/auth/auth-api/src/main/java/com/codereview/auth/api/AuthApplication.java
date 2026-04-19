package com.codereview.auth.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auth Service Application
 *
 * 独立认证服务主入口
 *
 * @author Auth Service Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.codereview.auth",
        "com.think.platform.shared"
})
@EntityScan(basePackages = "com.codereview.auth.core.domain")
@EnableJpaRepositories(basePackages = "com.codereview.auth.core.repository")
@EnableJpaAuditing
@EnableScheduling
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("""

                ========================================
                  认证服务启动成功！
                  API地址: http://localhost:8083/api
                  健康检查: http://localhost:8084/actuator/health
                ========================================
                """);
    }
}
