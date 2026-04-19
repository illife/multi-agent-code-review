package com.company.kb.core.service.cache;

import com.company.kb.core.service.QAService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 缓存服务
 * 提供Redis缓存操作
 */
@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存用户权限
     * @param userId 用户ID
     * @param accessibleDocIds 可访问文档ID列表
     */
    public void cacheUserPermissions(String userId, List<Long> accessibleDocIds) {
        String key = "user:permissions:" + userId;
        redisTemplate.opsForValue().set(key, accessibleDocIds, Duration.ofMinutes(5));
    }

    /**
     * 获取缓存的权限
     * @param userId 用户ID
     * @return 可访问文档ID列表
     */
    @SuppressWarnings("unchecked")
    public List<Long> getCachedUserPermissions(String userId) {
        String key = "user:permissions:" + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        return cached != null ? (List<Long>) cached : null;
    }

    /**
     * 缓存问答结果
     * @param questionHash 问题哈希
     * @param answer 答案
     */
    public void cacheQAAnswer(String questionHash, QAService.AnswerDTO answer) {
        String key = "qa:answer:" + questionHash;
        redisTemplate.opsForValue().set(key, answer, Duration.ofHours(1));
    }

    /**
     * 获取缓存的问答
     * @param questionHash 问题哈希
     * @return 答案
     */
    public QAService.AnswerDTO getCachedQAAnswer(String questionHash) {
        String key = "qa:answer:" + questionHash;
        Object cached = redisTemplate.opsForValue().get(key);
        return cached != null ? (QAService.AnswerDTO) cached : null;
    }

    /**
     * 缓存文档元数据
     * @param documentId 文档ID
     * @param metadata 元数据
     */
    public void cacheDocumentMetadata(Long documentId, Object metadata) {
        String key = "document:metadata:" + documentId;
        redisTemplate.opsForValue().set(key, metadata, Duration.ofMinutes(10));
    }

    /**
     * 获取缓存的文档元数据
     * @param documentId 文档ID
     * @return 元数据
     */
    public Object getCachedDocumentMetadata(Long documentId) {
        String key = "document:metadata:" + documentId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 清除用户缓存
     * @param userId 用户ID
     */
    public void evictUserCache(String userId) {
        String pattern = "user:permissions:" + userId;
        redisTemplate.delete(pattern);
    }

    /**
     * 清除文档缓存
     * @param documentId 文档ID
     */
    public void evictDocumentCache(Long documentId) {
        String pattern = "document:metadata:" + documentId;
        redisTemplate.delete(pattern);
    }

    /**
     * 清除所有缓存
     */
    public void evictAllCache() {
        redisTemplate.getConnectionFactory()
            .getConnection()
            .flushDb();
    }

    /**
     * 生成问题哈希
     * @param question 问题
     * @return 哈希值
     */
    public String hashQuestion(String question) {
        return String.valueOf(question.hashCode());
    }
}
