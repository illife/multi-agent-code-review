package com.company.kb.controller;

import com.think.platform.shared.common.result.Result;
import com.company.kb.core.service.RankFusionService;
import com.company.kb.core.service.VectorEmbeddingService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 搜索控制器
 * 提供BM25、KNN、混合搜索等API接口
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchService elasticsearchService;
    private final VectorEmbeddingService vectorEmbeddingService;
    private final RankFusionService rankFusionService;
    private final com.company.kb.core.service.QueryExpansionService queryExpansionService;

    /**
     * 默认搜索（混合搜索，使用RRF融合）
     * 这是 /search 根路径的默认实现
     * @param request 搜索请求
     * @return 混合搜索结果
     */
    @PostMapping
    public Result<HybridSearchResponse> search(
            @RequestBody SearchRequest request) {

        log.info("默认搜索(混合): query={}", request.getQuery());
        return searchHybrid(request);
    }

    /**
     * BM25关键词搜索
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/bm25")
    public Result<List<ElasticsearchService.SearchHit>> searchByBM25(
            @RequestBody SearchRequest request) {

        try {
            log.info("BM25搜索: query={}", request.getQuery());

            List<ElasticsearchService.SearchHit> results =
                elasticsearchService.searchByBM25(request.getQuery(), null);

            return Result.success(results);

        } catch (Exception e) {
            log.error("BM25搜索失败", e);
            return Result.failed(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * KNN向量搜索
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/knn")
    public Result<List<ElasticsearchService.SearchHit>> searchByKNN(
            @RequestBody SearchRequest request) {

        try {
            log.info("KNN搜索: query={}", request.getQuery());

            // 生成查询向量
            float[] queryVector = vectorEmbeddingService.generateEmbedding(request.getQuery());

            List<ElasticsearchService.SearchHit> results =
                elasticsearchService.searchByKNN(queryVector, null);

            return Result.success(results);

        } catch (Exception e) {
            log.error("KNN搜索失败", e);
            return Result.failed(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 混合搜索（使用RRF融合）
     * @param request 搜索请求
     * @return RRF融合后的搜索结果
     */
    @PostMapping("/hybrid")
    public Result<HybridSearchResponse> searchHybrid(
            @RequestBody SearchRequest request) {

        try {
            int page = request.getPage() != null && request.getPage() >= 0
                ? request.getPage()
                : 0;
            int size = request.getSize() != null && request.getSize() > 0
                ? request.getSize()
                : 10;

            log.info("混合搜索: query={}, page={}, size={}", request.getQuery(), page, size);

            // 0.5. 查询扩展（提高召回率）
            List<String> expandedQueries = queryExpansionService.expandQuery(request.getQuery());
            log.debug("查询扩展结果: {} -> {}", request.getQuery(), expandedQueries);

            // 0. 检查索引是否有数据
            long docCount = elasticsearchService.countDocuments();
            if (docCount == 0) {
                log.info("索引为空，返回空结果");
                HybridSearchResponse response = new HybridSearchResponse();
                response.setResults(new java.util.ArrayList<>());
                response.setTotal(0);
                response.setPage(page);
                response.setSize(size);
                response.setTotalPages(0);
                response.setQuery(request.getQuery());
                response.setBm25Count(0);
                response.setKnnCount(0);
                return Result.success(response);
            }

            // 1. 生成查询向量（使用原始查询）
            float[] queryVector = vectorEmbeddingService.generateEmbedding(request.getQuery());

            // 2. 并行执行BM25和KNN搜索（使用所有扩展查询）
            int searchSize = (page + 1) * size + 50;  // 多取一些以确保有足够结果

            // 对每个扩展查询执行BM25搜索
            List<CompletableFuture<List<ElasticsearchService.SearchHit>>> bm25Futures = new ArrayList<>();
            for (String query : expandedQueries) {
                CompletableFuture<List<ElasticsearchService.SearchHit>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return elasticsearchService.searchByBM25(
                            query,
                            null,
                            request.getDepartments(),
                            request.getTags(),
                            request.getFileTypes(),
                            request.getDateFrom(),
                            request.getDateTo(),
                            0,
                            searchSize
                        );
                    } catch (Exception e) {
                        log.error("BM25搜索失败: query={}", query, e);
                        return new ArrayList<>();
                    }
                });
                bm25Futures.add(future);
            }

            // KNN搜索（只使用原始查询的向量）
            CompletableFuture<List<ElasticsearchService.SearchHit>> knnFuture =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return elasticsearchService.searchByKNN(
                            queryVector,
                            null,
                            request.getDepartments(),
                            request.getTags(),
                            request.getFileTypes(),
                            request.getDateFrom(),
                            request.getDateTo(),
                            0,
                            searchSize
                        );
                    } catch (Exception e) {
                        log.error("KNN搜索失败", e);
                        return new ArrayList<>();
                    }
                });

            // 等待所有搜索完成
            CompletableFuture<Void> allBm25Futures = CompletableFuture.allOf(
                bm25Futures.toArray(new CompletableFuture[0])
            );
            CompletableFuture.allOf(allBm25Futures, knnFuture).join();

            // 合并所有BM25结果（去重）
            Map<String, ElasticsearchService.SearchHit> bm25Merged = new LinkedHashMap<>();
            for (CompletableFuture<List<ElasticsearchService.SearchHit>> future : bm25Futures) {
                List<ElasticsearchService.SearchHit> results = future.join();
                for (ElasticsearchService.SearchHit hit : results) {
                    String key = hit.getDocumentId() + "_" + hit.getId();
                    if (!bm25Merged.containsKey(key)) {
                        bm25Merged.put(key, hit);
                    }
                }
            }
            List<ElasticsearchService.SearchHit> bm25Results = new ArrayList<>(bm25Merged.values());
            List<ElasticsearchService.SearchHit> knnResults = knnFuture.join();

            log.info("BM25结果数: {} (来自{}个查询), KNN结果数: {}",
                bm25Results.size(), expandedQueries.size(), knnResults.size());

            // 3. 智能融合：BM25优先策略
            // 对于精确查询（如专有名词），只要BM25有结果就只用BM25
            // KNN容易将语义相近但不相关的文档也检索出来
            List<ElasticsearchService.SearchHit> fusedResults;
            if (!bm25Results.isEmpty()) {
                // BM25有结果时，优先使用BM25的精确匹配结果
                log.info("BM25有结果({}个)，仅使用BM25精确匹配", bm25Results.size());
                fusedResults = bm25Results;
            } else {
                // BM25无结果时，才使用KNN语义搜索
                log.info("BM25无结果，使用KNN语义搜索");
                fusedResults = knnResults;
            }

            // 4. 应用分页
            int totalResults = fusedResults.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalResults);

            List<ElasticsearchService.SearchHit> pagedResults = fusedResults.subList(
                fromIndex,
                toIndex
            );

            // 5. 计算总页数
            int totalPages = (int) Math.ceil((double) totalResults / size);

            // 6. 构建响应
            HybridSearchResponse response = new HybridSearchResponse();
            response.setResults(pagedResults);
            response.setTotal(totalResults);
            response.setPage(page);
            response.setSize(size);
            response.setTotalPages(totalPages);
            response.setQuery(request.getQuery());
            response.setBm25Count(bm25Results.size());
            response.setKnnCount(knnResults.size());

            log.info("混合搜索完成: page={}, totalResults={}, totalPages={}, topScore={}",
                page, totalResults, totalPages,
                pagedResults.isEmpty() ? 0 : pagedResults.get(0).getRrfScore());

            // 7. 异步保存搜索历史（不阻塞响应）
            saveSearchHistoryAsync(
                1L, // TODO: 从Authentication中获取真实userId
                request.getQuery(),
                totalResults,
                "HYBRID",
                request
            );

            return Result.success(response);

        } catch (Exception e) {
            log.error("混合搜索失败", e);
            return Result.failed(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 异步保存搜索历史
     */
    @Async
    public void saveSearchHistoryAsync(
            Long userId,
            String query,
            Integer resultCount,
            String searchType,
            SearchRequest request) {

        try {
            Map<String, Object> filters = new HashMap<>();
            if (request.getDepartments() != null && !request.getDepartments().isEmpty()) {
                filters.put("departments", request.getDepartments());
            }
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                filters.put("tags", request.getTags());
            }
            if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
                filters.put("fileTypes", request.getFileTypes());
            }
            if (request.getDateFrom() != null || request.getDateTo() != null) {
                Map<String, String> dateRange = new HashMap<>();
                if (request.getDateFrom() != null) {
                    dateRange.put("from", request.getDateFrom());
                }
                if (request.getDateTo() != null) {
                    dateRange.put("to", request.getDateTo());
                }
                filters.put("dateRange", dateRange);
            }

            // TODO: Save search history - SearchHistoryService removed
            log.debug("搜索历史保存跳过: query={}", query);

        } catch (Exception e) {
            log.error("保存搜索历史失败: query={}", query, e);
            // 不抛出异常，避免影响搜索结果返回
        }
    }

    /**
     * 搜索请求DTO
     */
    public static class SearchRequest {
        private String query;
        private Integer size;
        private Integer page;  // 分页参数（从0开始）
        // 筛选参数
        private List<String> departments;   // 部门筛选
        private List<String> tags;          // 标签筛选
        private List<String> fileTypes;     // 文件类型筛选（pdf, docx, xlsx等）
        private String dateFrom;            // 起始日期（yyyy-MM-dd）
        private String dateTo;              // 结束日期（yyyy-MM-dd）

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public List<String> getDepartments() { return departments; }
        public void setDepartments(List<String> departments) { this.departments = departments; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public List<String> getFileTypes() { return fileTypes; }
        public void setFileTypes(List<String> fileTypes) { this.fileTypes = fileTypes; }
        public String getDateFrom() { return dateFrom; }
        public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
        public String getDateTo() { return dateTo; }
        public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    }

    /**
     * 混合搜索响应DTO
     */
    public static class HybridSearchResponse {
        private List<ElasticsearchService.SearchHit> results;
        private Integer total;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private String query;
        private Integer bm25Count;
        private Integer knnCount;

        // Getters and Setters
        public List<ElasticsearchService.SearchHit> getResults() { return results; }
        public void setResults(List<ElasticsearchService.SearchHit> results) { this.results = results; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getBm25Count() { return bm25Count; }
        public void setBm25Count(Integer bm25Count) { this.bm25Count = bm25Count; }
        public Integer getKnnCount() { return knnCount; }
        public void setKnnCount(Integer knnCount) { this.knnCount = knnCount; }
    }
}
