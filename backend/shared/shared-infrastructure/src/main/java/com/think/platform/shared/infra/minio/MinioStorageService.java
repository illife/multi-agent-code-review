package com.think.platform.shared.infra.minio;

import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MinIO Storage Service
 * 统一的文件存储服务
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传文件到指定 bucket
     */
    public String uploadFile(String bucketName, String objectName, InputStream stream, Long size, String contentType) {
        try {
            // 确保 bucket 存在
            ensureBucketExists(bucketName);

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, size, minioProperties.getPartSize())
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded successfully: {}/{}", bucketName, objectName);
            return getObjectUrl(bucketName, objectName);

        } catch (Exception e) {
            log.error("Failed to upload file: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * 上传文件 (使用 MultipartFile)
     */
    public String uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            return uploadFile(bucketName, objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("Failed to upload MultipartFile: {}", objectName, e);
            throw new RuntimeException("MultipartFile upload failed", e);
        }
    }

    /**
     * 上传文件到默认 bucket
     */
    public String uploadFile(String objectName, InputStream stream, Long size, String contentType) {
        return uploadFile(minioProperties.getBucketName(), objectName, stream, size, contentType);
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("File download failed", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("File deleted: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("File deletion failed", e);
        }
    }

    /**
     * 批量删除文件
     */
    public void deleteFiles(String bucketName, List<String> objectNames) {
        try {
            List<DeleteObject> objects = objectNames.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objects)
                            .build()
            );

            // 检查删除结果
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("Failed to delete object: {} - {}", error.objectName(), error.message());
            }

            log.info("Batch delete completed: {}/{}", bucketName, objectNames.size());

        } catch (Exception e) {
            log.error("Failed to batch delete files: {}/{}", bucketName, objectNames.size(), e);
            throw new RuntimeException("Batch file deletion failed", e);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件 URL
     */
    public String getObjectUrl(String bucketName, String objectName) {
        return String.format("%s/%s/%s", minioProperties.getEndpoint(), bucketName, objectName);
    }

    /**
     * 确保 bucket 存在
     */
    private void ensureBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Bucket created: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucketName, e);
            throw new RuntimeException("Bucket creation failed", e);
        }
    }

    /**
     * 列出所有 buckets
     */
    public List<String> listBuckets() {
        try {
            return minioClient.listBuckets().stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list buckets", e);
            throw new RuntimeException("List buckets failed", e);
        }
    }

    /**
     * 异步上传文件
     */
    public CompletableFuture<String> uploadFileAsync(String bucketName, String objectName, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> uploadFile(bucketName, objectName, file));
    }
}
