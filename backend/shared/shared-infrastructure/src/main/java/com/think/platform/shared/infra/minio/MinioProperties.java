package com.think.platform.shared.infra.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO Configuration Properties
 *
 * @author AI Code Mentor Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 服务端点
     */
    private String endpoint = "http://localhost:9000";

    /**
     * MinIO 访问密钥
     */
    private String accessKey = "minioadmin";

    /**
     * MinIO 密钥
     */
    private String secretKey = "minioadmin";

    /**
     * 默认 bucket 名称
     */
    private String bucketName = "ai-mentor-documents";

    /**
     * 项目文件 bucket 名称
     */
    private String projectBucketName = "ai-mentor-projects";

    /**
     * 分片上传时的分片大小 (字节)
     */
    private Long partSize = 5242880L;  // 5MB

    /**
     * 连接超时时间 (毫秒)
     */
    private Long connectTimeout = 10000L;

    /**
     * 写入超时时间 (毫秒)
     */
    private Long writeTimeout = 60000L;

    /**
     * 读取超时时间 (毫秒)
     */
    private Long readTimeout = 10000L;
}
