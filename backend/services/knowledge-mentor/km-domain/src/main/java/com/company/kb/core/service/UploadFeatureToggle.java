package com.company.kb.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 上传功能开关服务
 * 用于灰度发布和功能控制
 *
 * 功能：
 * 1. 大文件强制使用分片上传（50MB+）
 * 2. 通过配置开关控制小文件是否使用分片上传
 * 3. 支持基于百分比的用户灰度发布
 *
 * @author Knowledge Base Team
 */
@Service
@Slf4j
public class UploadFeatureToggle {

    /**
     * 分片上传功能开关
     * true: 启用分片上传
     * false: 使用传统上传
     */
    @Value("${app.upload.chunked-upload.enabled:false}")
    private boolean chunkedUploadEnabled;

    /**
     * 大文件分片上传阈值
     * 超过此大小的文件强制使用分片上传
     */
    @Value("${app.upload.chunked-upload.min-size-threshold:52428800}")
    private long minSizeThreshold;  // 默认50MB

    /**
     * 灰度发布百分比（0-100）
     * 0: 不启用
     * 100: 全部启用
     * 1-99: 随机百分比用户启用
     */
    @Value("${app.upload.chunked-upload.rollout-percentage:0}")
    private int rolloutPercentage;

    /**
     * 判断是否应该使用分片上传
     *
     * 规则：
     * 1. 大文件（>= minSizeThreshold）强制使用分片上传
     * 2. 小文件根据功能开关和灰度百分比决定
     *
     * @param fileSize 文件大小（字节）
     * @return true表示使用分片上传，false表示使用传统上传
     */
    public boolean shouldUseChunkedUpload(long fileSize) {
        // 大文件强制使用分片上传（50MB+）
        if (fileSize >= minSizeThreshold) {
            log.debug("文件超过阈值，强制使用分片上传: size={}, threshold={}",
                fileSize, minSizeThreshold);
            return true;
        }

        // 功能开关
        if (chunkedUploadEnabled) {
            log.debug("功能开关已启用，使用分片上传");
            return true;
        }

        // 灰度发布：根据百分比随机决定
        if (rolloutPercentage > 0 && rolloutPercentage < 100) {
            int random = ThreadLocalRandom.current().nextInt(100);
            boolean useChunked = random < rolloutPercentage;

            log.debug("灰度发布: random={}, percentage={}, useChunked={}",
                random, rolloutPercentage, useChunked);

            return useChunked;
        }

        // 默认不使用分片上传
        log.debug("使用传统上传: size={}", fileSize);
        return false;
    }

    /**
     * 判断是否应该使用分片上传（基于用户ID的灰度）
     * 确保同一用户的体验一致
     *
     * @param fileSize 文件大小
     * @param userId 用户ID
     * @return true表示使用分片上传
     */
    public boolean shouldUseChunkedUpload(long fileSize, String userId) {
        // 大文件强制使用分片上传
        if (fileSize >= minSizeThreshold) {
            return true;
        }

        // 功能开关
        if (chunkedUploadEnabled) {
            return true;
        }

        // 灰度发布：基于用户ID的哈希值
        if (rolloutPercentage > 0 && rolloutPercentage < 100) {
            int userHash = Math.abs(userId.hashCode() % 100);
            boolean useChunked = userHash < rolloutPercentage;

            log.debug("基于用户ID的灰度发布: userId={}, userHash={}, percentage={}, useChunked={}",
                userId, userHash, rolloutPercentage, useChunked);

            return useChunked;
        }

        return false;
    }

    /**
     * 获取当前配置状态
     */
    public FeatureToggleStatus getStatus() {
        return FeatureToggleStatus.builder()
            .chunkedUploadEnabled(chunkedUploadEnabled)
            .minSizeThreshold(minSizeThreshold)
            .rolloutPercentage(rolloutPercentage)
            .build();
    }

    /**
     * 功能开关状态
     */
    @lombok.Data
    @lombok.Builder
    public static class FeatureToggleStatus {
        private boolean chunkedUploadEnabled;
        private long minSizeThreshold;
        private int rolloutPercentage;
    }

    // Getters for configuration values
    public boolean isChunkedUploadEnabled() {
        return chunkedUploadEnabled;
    }

    public long getMinSizeThreshold() {
        return minSizeThreshold;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }
}
