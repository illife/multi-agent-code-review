package com.company.kb.core.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分片状态追踪服务
 * 使用Redis BitMap追踪分片上传状态，实现高效的断点续传
 *
 * BitMap说明：
 * - 每个位对应一个分片的状态
 * - 0表示未上传，1表示已上传
 * - 内存占用极小：200个分片仅需25字节
 *
 * @author Knowledge Base Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkStatusService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UPLOAD_STATUS_PREFIX = "upload:status:";
    private static final String UPLOAD_META_PREFIX = "upload:meta:";
    private static final int DEFAULT_EXPIRE_HOURS = 24;

    /**
     * 初始化分片状态
     * 创建一个新的BitMap，所有位初始化为0
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     */
    public void initChunkStatus(String uploadId, Integer totalChunks) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;

        // 初始化BitMap，所有位设为0
        // Redis BitMap会自动扩展，所以不需要预先设置所有位
        redisTemplate.opsForValue().setBit(statusKey, 0, false);

        // 设置过期时间（24小时）
        redisTemplate.expire(statusKey, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);

        log.debug("初始化分片状态: uploadId={}, totalChunks={}", uploadId, totalChunks);
    }

    /**
     * 标记分片已上传
     * 将指定分片对应的位设置为1
     *
     * @param uploadId 上传会话ID
     * @param chunkNumber 分片编号（从0开始）
     * @return 是否之前未上传（true表示新上传，false表示已存在）
     */
    public Boolean markChunkUploaded(String uploadId, Integer chunkNumber) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;
        Boolean result = redisTemplate.opsForValue().setBit(statusKey, chunkNumber, true);

        log.debug("标记分片已上传: uploadId={}, chunk={}, wasPreviouslyUploaded={}",
            uploadId, chunkNumber, !result);

        // result为true表示之前是0（新上传），false表示之前是1（已存在）
        return result;
    }

    /**
     * 检查分片是否已上传
     *
     * @param uploadId 上传会话ID
     * @param chunkNumber 分片编号
     * @return true表示已上传，false表示未上传
     */
    public Boolean isChunkUploaded(String uploadId, Integer chunkNumber) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;
        return redisTemplate.opsForValue().getBit(statusKey, chunkNumber);
    }

    /**
     * 获取已上传分片列表
     * 遍历BitMap，返回所有已上传的分片编号
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @return 已上传分片编号列表
     */
    public List<Integer> getUploadedChunks(String uploadId, Integer totalChunks) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;
        List<Integer> uploadedChunks = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(statusKey, i))) {
                uploadedChunks.add(i);
            }
        }

        log.debug("获取已上传分片: uploadId={}, uploaded={}/{}",
            uploadId, uploadedChunks.size(), totalChunks);

        return uploadedChunks;
    }

    /**
     * 检查所有分片是否已上传
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @return true表示所有分片已上传，false表示还有未上传的分片
     */
    public Boolean isAllChunksUploaded(String uploadId, Integer totalChunks) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;

        for (int i = 0; i < totalChunks; i++) {
            if (!Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(statusKey, i))) {
                log.debug("检查所有分片: uploadId={}, chunk={} 未上传", uploadId, i);
                return false;
            }
        }

        log.debug("检查所有分片: uploadId={}, all={} 已上传", uploadId, totalChunks);
        return true;
    }

    /**
     * 获取上传进度
     * 返回已上传分片数量
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @return 已上传分片数
     */
    public Integer getUploadProgress(String uploadId, Integer totalChunks) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;

        long uploadedCount = 0;
        for (int i = 0; i < totalChunks; i++) {
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(statusKey, i))) {
                uploadedCount++;
            }
        }

        return (int) uploadedCount;
    }

    /**
     * 获取上传进度百分比
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @return 进度百分比（0-100）
     */
    public int getUploadProgressPercentage(String uploadId, Integer totalChunks) {
        if (totalChunks == null || totalChunks == 0) {
            return 0;
        }

        int uploadedCount = getUploadProgress(uploadId, totalChunks);
        return (int) ((uploadedCount * 100.0) / totalChunks);
    }

    /**
     * 获取缺失的分片列表
     * 返回所有未上传的分片编号，用于断点续传
     *
     * @param uploadId 上传会话ID
     * @param totalChunks 总分片数
     * @return 未上传分片编号列表
     */
    public List<Integer> getMissingChunks(String uploadId, Integer totalChunks) {
        String statusKey = UPLOAD_STATUS_PREFIX + uploadId;
        List<Integer> missingChunks = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            if (!Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(statusKey, i))) {
                missingChunks.add(i);
            }
        }

        log.debug("获取缺失分片: uploadId={}, missing={}/{}",
            uploadId, missingChunks.size(), totalChunks);

        return missingChunks;
    }

    /**
     * 清理上传会话
     * 删除与上传会话相关的所有Redis数据
     *
     * @param uploadId 上传会话ID
     */
    public void cleanupUploadSession(String uploadId) {
        redisTemplate.delete(UPLOAD_STATUS_PREFIX + uploadId);
        redisTemplate.delete(UPLOAD_META_PREFIX + uploadId);
        log.info("清理上传会话: uploadId={}", uploadId);
    }

    /**
     * 保存上传会话元数据
     *
     * @param uploadId 上传会话ID
     * @param metadata 元数据
     */
    public void saveMetadata(String uploadId, Object metadata) {
        String metaKey = UPLOAD_META_PREFIX + uploadId;
        redisTemplate.opsForValue().set(metaKey, metadata, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("保存上传元数据: uploadId={}", uploadId);
    }

    /**
     * 获取上传会话元数据
     *
     * @param uploadId 上传会话ID
     * @param clazz 元数据类型
     * @return 元数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String uploadId, Class<T> clazz) {
        String metaKey = UPLOAD_META_PREFIX + uploadId;
        Object metadata = redisTemplate.opsForValue().get(metaKey);
        if (metadata != null && clazz.isInstance(metadata)) {
            return (T) metadata;
        }
        return null;
    }
}
