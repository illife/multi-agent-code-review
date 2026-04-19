package com.codereview.ai.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Code Review AI Application Entry Point
 *
 * Multi-agent AI-powered code review and teaching system
 *
 * @author Code Review AI Team
 */
@SpringBootApplication(scanBasePackages = {"com.codereview.ai", "com.think.platform.shared.ai", "com.think.platform.shared"})
@EntityScan(basePackages = {"com.codereview.ai.domain.model"})
@EnableJpaRepositories(basePackages = {"com.codereview.ai.domain.repository"})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableKafka
public class CodeIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeIntelligenceApplication.class, args);
    }
}
