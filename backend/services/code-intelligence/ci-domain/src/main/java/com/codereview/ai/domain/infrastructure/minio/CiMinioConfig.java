package com.codereview.ai.domain.infrastructure.minio;

import com.think.platform.shared.infra.minio.MinioProperties;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类 - 代码审查模块
 * 配置代码审查专用的MinioService Bean
 *
 * @author Code Review AI Team
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
public class CiMinioConfig {

    private final MinioProperties minioProperties;

    /**
     * 创建代码审查专用的MinioService Bean
     * MinioClient由shared-infrastructure的MinioConfig提供
     */
    @Bean
    public MinioService minioService(MinioClient minioClient) {
        return new MinioService(minioClient, minioProperties);
    }
}
