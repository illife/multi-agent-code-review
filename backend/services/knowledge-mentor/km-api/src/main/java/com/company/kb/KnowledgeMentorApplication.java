package com.company.kb;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Knowledge Mentor Service - 知识库服务主启动类
 * Multi-Agent Platform的一部分，负责文档管理和RAG检索
 *
 * @author Knowledge Mentor Team
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {
        "com.company.kb",
        "com.think.platform.shared"
})
@EntityScan(basePackages = "com.company.kb.core.domain")
@EnableJpaRepositories(basePackages = "com.company.kb.core.repository")
@EnableJpaAuditing
@EnableScheduling
public class KnowledgeMentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeMentorApplication.class, args);
        System.out.println("""

                ========================================
                  Knowledge Mentor Service Started!
                  API: http://localhost:8080/api
                  Health: http://localhost:8080/api/actuator/health
                ========================================
                """);
    }
}
