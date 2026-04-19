package com.company.kb.infra.elasticsearch.service;

import com.company.kb.infra.elasticsearch.DocumentChunk;

import java.util.List;

/**
 * Elasticsearch服务接口
 * 提供BM25关键词检索、KNN向量检索、混合检索和索引管理
 */
public interface ElasticsearchService {

    /**
     * 创建索引
     */
    void createIndex(String indexName) throws Exception;

    /**
     * 删除索引
     */
    void deleteIndex(String indexName) throws Exception;

    /**
     * 检查索引是否存在
     */
    boolean indexExists(String indexName) throws Exception;

    /**
     * BM25关键词检索
     */
    List<SearchHit> searchByBM25(String query, List<Long> accessibleDocIds) throws Exception;

    /**
     * BM25关键词检索（带筛选）
     */
    List<SearchHit> searchByBM25(
        String query,
        List<Long> accessibleDocIds,
        List<String> departments,
        List<String> tags,
        List<String> fileTypes,
        String dateFrom,
        String dateTo
    ) throws Exception;

    /**
     * BM25关键词检索（带筛选和分页）
     */
    List<SearchHit> searchByBM25(
        String query,
        List<Long> accessibleDocIds,
        List<String> departments,
        List<String> tags,
        List<String> fileTypes,
        String dateFrom,
        String dateTo,
        int from,
        int size
    ) throws Exception;

    /**
     * KNN向量检索
     */
    List<SearchHit> searchByKNN(float[] queryVector, List<Long> accessibleDocIds) throws Exception;

    /**
     * KNN向量检索（带筛选）
     */
    List<SearchHit> searchByKNN(
        float[] queryVector,
        List<Long> accessibleDocIds,
        List<String> departments,
        List<String> tags,
        List<String> fileTypes,
        String dateFrom,
        String dateTo
    ) throws Exception;

    /**
     * KNN向量检索（带筛选和分页）
     */
    List<SearchHit> searchByKNN(
        float[] queryVector,
        List<Long> accessibleDocIds,
        List<String> departments,
        List<String> tags,
        List<String> fileTypes,
        String dateFrom,
        String dateTo,
        int from,
        int size
    ) throws Exception;

    /**
     * 批量索引文档块
     */
    void bulkIndexChunks(List<DocumentChunk> chunks) throws Exception;

    /**
     * 删除文档的所有块
     */
    void deleteDocumentChunks(Long documentId) throws Exception;

    /**
     * 统计索引中的文档数量
     */
    long countDocuments() throws Exception;

    /**
     * 搜索结果
     */
    class SearchHit {
        private String id;
        private double score;
        private String content;
        private String title;
        private String fileName;
        private Long documentId;
        private double rrfScore;
        private String highlight; // Elasticsearch原生高亮结果


        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public double getRrfScore() { return rrfScore; }
        public void setRrfScore(double rrfScore) { this.rrfScore = rrfScore; }
        public String getHighlight() { return highlight; }
        public void setHighlight(String highlight) { this.highlight = highlight; }
    }
}
