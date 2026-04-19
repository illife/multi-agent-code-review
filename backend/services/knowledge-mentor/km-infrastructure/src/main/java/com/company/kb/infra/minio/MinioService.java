package com.company.kb.infra.minio;

import com.think.platform.shared.infra.minio.MinioProperties;
import io.minio.*;
import io.minio.messages.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO服务类
 * 提供文件上传、下载、合并、删除等操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 初始化MinIO，确保存储桶存在
     */
    @PostConstruct
    public void init() {
        try {
            ensureBucketExists();
            log.info("MinIO初始化成功: bucket={}", minioProperties.getBucketName());
        } catch (Exception e) {
            log.error("MinIO初始化失败", e);
            throw new RuntimeException("MinIO初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确保存储桶存在，不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName())
                .build()
        );

        if (!found) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build()
            );
            log.info("MinIO bucket创建成功: {}", minioProperties.getBucketName());
        }
    }

    /**
     * 上传文件（用于普通上传）
     *
     * @param inputStream 文件输入流
     * @param objectName 对象名称
     * @param size 文件大小
     * @param contentType 内容类型
     * @return 文件在MinIO中的路径
     */
    public String uploadFile(InputStream inputStream, String objectName,
                             long size, String contentType) throws Exception {
        // 使用UUID作为目录前缀，避免文件名冲突
        String uniqueId = java.util.UUID.randomUUID().toString();
        String objectPath = String.format("documents/%s/%s", uniqueId, objectName);

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectPath)
                .stream(inputStream, size, minioProperties.getPartSize())
                .contentType(contentType)
                .build()
        );

        log.info("文件上传成功: objectName={}, path={}", objectName, objectPath);

        return objectPath;
    }

    /**
     * 上传分片
     *
     * @param uploadId 上传会话ID
     * @param chunkNumber 分片编号
     * @param inputStream 分片数据流
     * @param size 分片大小
     * @param contentType 内容类型
     * @return 分片对象路径
     */
    public String uploadChunk(String uploadId, Integer chunkNumber,
                             InputStream inputStream, long size, String contentType) throws Exception {
        String objectName = String.format("documents/%s/chunks/%d", uploadId, chunkNumber);

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectName)
                .stream(inputStream, size, minioProperties.getPartSize())
                .contentType(contentType)
                .build()
        );

        log.debug("分片上传成功: uploadId={}, chunk={}, path={}",
            uploadId, chunkNumber, objectName);

        return objectName;
    }

    /**
     * 合并分片
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @param finalFileName 最终文件名
     * @return 合并后的文件路径
     */
    public String mergeChunks(String uploadId, Integer totalChunks, String finalFileName) throws Exception {
        // 构建分片列表
        List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            String chunkPath = String.format("documents/%s/chunks/%d", uploadId, i);
            sources.add(
                ComposeSource.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(chunkPath)
                    .build()
            );
        }

        // 合并后的文件路径（使用UUID避免冲突）
        String uniqueId = java.util.UUID.randomUUID().toString();
        String finalPath = String.format("documents/%s/%s", uniqueId, finalFileName);

        // 执行合并
        minioClient.composeObject(
            ComposeObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(finalPath)
                .sources(sources)
                .build()
        );

        log.info("分片合并成功: uploadId={}, finalPath={}", uploadId, finalPath);

        // 删除临时分片
        for (int i = 0; i < totalChunks; i++) {
            String chunkPath = String.format("documents/%s/chunks/%d", uploadId, i);
            try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(chunkPath)
                        .build()
                );
            } catch (Exception e) {
                log.warn("删除分片失败: {}", chunkPath, e);
            }
        }

        return finalPath;
    }

    /**
     * 下载文件到临时目录
     * 用于Consumer处理MinIO中的文件
     *
     * @param objectPath 对象路径
     * @return 临时文件
     */
    public File downloadToTempFile(String objectPath) throws Exception {
        // 创建临时文件
        String suffix = objectPath.substring(objectPath.lastIndexOf("."));
        Path tempFile = Files.createTempFile("minio_", suffix);

        // 下载文件
        try (InputStream stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectPath)
                .build()
        )) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("文件下载到临时目录: objectPath={}, tempFile={}",
            objectPath, tempFile);

        return tempFile.toFile();
    }

    /**
     * 下载文件为Resource对象
     * 用于Controller返回文件内容
     *
     * @param objectPath 对象路径
     * @return 文件资源
     */
    public Resource downloadFile(String objectPath) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectPath)
                    .build()
        )) {
            // 将InputStream读取到byte数组
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384]; // 16KB buffer
            int nRead;
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();

            // 提取文件名
            String filename = objectPath.substring(objectPath.lastIndexOf("/") + 1);

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        }
    }

    /**
     * 删除文件
     *
     * @param objectPath 对象路径
     */
    public void deleteFile(String objectPath) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectPath)
                .build()
        );
        log.debug("文件删除成功: {}", objectPath);
    }

    /**
     * 获取文件文本内容
     * 用于文本文件预览
     *
     * @param objectPath 对象路径
     * @return 文本内容
     */
    public String getTextContent(String objectPath) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectPath)
                    .build()
        )) {
            // 限制文件大小，避免加载过大的文件
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int totalBytes = 0;
            int maxBytes = 1024 * 1024; // 最大1MB

            int nRead;
            while ((nRead = stream.read(buffer, 0, buffer.length)) != -1) {
                totalBytes += nRead;
                if (totalBytes > maxBytes) {
                    throw new IOException("文件过大，不支持预览（最大1MB）");
                }
                byteBuffer.write(buffer, 0, nRead);
            }

            return byteBuffer.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取临时访问URL
     *
     * @param objectPath 对象路径
     * @param expires 过期时间（秒）
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectPath, int expires) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectPath)
                .expiry(expires)
                .build()
        );
    }

    /**
     * 检查对象是否存在
     *
     * @param objectPath 对象路径
     * @return 是否存在
     */
    public boolean objectExists(String objectPath) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectPath)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
