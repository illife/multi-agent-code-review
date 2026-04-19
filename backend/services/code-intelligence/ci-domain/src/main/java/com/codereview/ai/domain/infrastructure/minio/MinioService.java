package com.codereview.ai.domain.infrastructure.minio;

import com.think.platform.shared.infra.minio.MinioProperties;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * MinIO文件存储服务 - 代码审查模块
 * 处理项目ZIP文件的上传、下载和管理
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @PostConstruct
    public void init() {
        try {
            // 确保存储桶存在
            ensureBucketExists(minioProperties.getProjectBucketName());
            log.info("✅ MinioService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinioService", e);
        }
    }

    /**
     * 上传项目ZIP文件到MinIO
     *
     * @param file ZIP文件
     * @param objectName 对象名称
     * @return MinIO对象路径
     */
    public String uploadProjectFile(File file, String objectName) {
        try {
            String bucketName = minioProperties.getProjectBucketName();
            String path = "projects/" + objectName;

            try (InputStream inputStream = new FileInputStream(file)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(path)
                                .stream(inputStream, file.length(), minioProperties.getPartSize())
                                .contentType("application/zip")
                                .build()
                );
            }

            log.info("Project file uploaded: {} (size: {} bytes)", path, file.length());
            return path;
        } catch (Exception e) {
            log.error("Failed to upload project file: {}", objectName, e);
            throw new RuntimeException("Failed to upload project file", e);
        }
    }

    /**
     * 从MinIO下载项目ZIP文件到临时文件
     *
     * @param minioPath MinIO对象路径
     * @return 临时文件
     */
    public File downloadToTempFile(String minioPath) {
        try {
            String bucketName = minioProperties.getProjectBucketName();

            // 创建临时文件 with unique name
            String suffix = minioPath.contains(".") ? minioPath.substring(minioPath.lastIndexOf('.')) : ".zip";
            String uniquePrefix = "project-download-" + java.util.UUID.randomUUID().toString() + "-";
            File tempFile = File.createTempFile(uniquePrefix, suffix);

            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(minioPath)
                            .build()
            )) {
                // Copy stream to temp file with REPLACE_EXISTING option
                java.nio.file.Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Project file downloaded: {} -> {}", minioPath, tempFile.getAbsolutePath());
            return tempFile;
        } catch (Exception e) {
            log.error("Failed to download project file: {}", minioPath, e);
            throw new RuntimeException("Failed to download project file", e);
        }
    }

    /**
     * 删除MinIO中的对象
     *
     * @param minioPath MinIO对象路径
     */
    public void deleteObject(String minioPath) {
        try {
            String bucketName = minioProperties.getProjectBucketName();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(minioPath)
                            .build()
            );
            log.info("Object deleted from MinIO: {}", minioPath);
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: {}", minioPath, e);
        }
    }

    /**
     * 生成预签名上传URL（用于前端直传）
     *
     * @param objectName 对象名称
     * @param expirySeconds 过期时间（秒）
     * @return 预签名URL
     */
    public String generatePresignedUploadUrl(String objectName, int expirySeconds) {
        try {
            String bucketName = minioProperties.getProjectBucketName();
            String path = "projects/" + objectName;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(path)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", objectName, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * 获取对象信息
     *
     * @param minioPath MinIO对象路径
     * @return 对象信息
     */
    public StatObjectResponse getFileInfo(String minioPath) {
        try {
            String bucketName = minioProperties.getProjectBucketName();
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(minioPath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get file info: {}", minioPath, e);
            throw new RuntimeException("Failed to get file info", e);
        }
    }

    /**
     * 检查对象是否存在
     *
     * @param minioPath MinIO对象路径
     * @return 是否存在
     */
    public boolean objectExists(String minioPath) {
        try {
            getFileInfo(minioPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created new bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucketName, e);
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }
    }
}
