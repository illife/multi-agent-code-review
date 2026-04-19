# 搜索功能MVP开发指南

本文档为搜索功能MVP开发的详细技术指南，包含完整的技术方案、代码示例和实施步骤。

---

## 开发概览

### MVP核心功能（优先级P0）

| 功能 | 前端工时 | 后端工时 | 总工时 | 状态 |
|------|----------|----------|--------|------|
| RRF融合算法 | - | 2h | 2h | ❌ |
| 混合搜索API优化 | - | 1h | 1h | ❌ |
| searchService.ts | 1h | - | 1h | ❌ |
| 前端搜索结果展示 | 2h | - | 2h | ❌ |
| **P0合计** | **3h** | **3h** | **6h** | - |

### 次要功能（优先级P1）

| 功能 | 前端工时 | 后端工时 | 总工时 | 状态 |
|------|----------|----------|--------|------|
| 搜索高亮 | 2h | 1h | 3h | ❌ |
| 高级筛选 | 3h | 1h | 4h | ❌ |
| 分页功能 | 1h | - | 1h | 🔶 |
| **P1合计** | **6h** | **2h** | **8h** | - |

---

## 模块一：后端 - RRF融合算法

### 任务说明

实现Reciprocal Rank Fusion (RRF)算法，用于融合BM25关键词搜索和KNN向量搜索的结果。

### 技术背景

**RRF算法公式**：
```
RRF_score(d) = Σ (k / (k + rank_i(d)))

其中：
- d: 文档
- k: 常数（通常取60）
- rank_i(d): 文档d在第i个结果列表中的排名（从1开始）
- 如果文档不在列表中，rank = ∞，该项为0
```

**优势**：
- 不需要归一化分数
- 对异常值不敏感
- 实现简单，效果好

### 实现步骤

#### 步骤1：创建RankFusionService接口

**文件位置**：`backend/knowledge-base-core/src/main/java/com/company/kb/core/service/RankFusionService.java`

```java
package com.company.kb.core.service;

import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import java.util.List;

/**
 * 排序融合服务接口
 * 提供多种搜索结果融合算法
 */
public interface RankFusionService {

    /**
     * RRF融合算法
     * @param results1 第一个结果列表（通常是BM25结果）
     * @param results2 第二个结果列表（通常是KNN结果）
     * @return 融合后的结果列表，按RRF分数降序排序
     */
    List<ElasticsearchService.SearchHit> reciprocalRankFusion(
        List<ElasticsearchService.SearchHit> results1,
        List<ElasticsearchService.SearchHit> results2
    );

    /**
     * RRF融合算法（可配置k值）
     * @param results1 第一个结果列表
     * @param results2 第二个结果列表
     * @param k RRF常数（默认60）
     * @return 融合后的结果列表
     */
    List<ElasticsearchService.SearchHit> reciprocalRankFusion(
        List<ElasticsearchService.SearchHit> results1,
        List<ElasticsearchService.SearchHit> results2,
        int k
    );

    /**
     * 加权平均融合（需要归一化分数）
     * @param results1 第一个结果列表
     * @param results2 第二个结果列表
     * @param weight1 第一个列表的权重
     * @param weight2 第二个列表的权重
     * @return 融合后的结果列表
     */
    List<ElasticsearchService.SearchHit> weightedFusion(
        List<ElasticsearchService.SearchHit> results1,
        List<ElasticsearchService.SearchHit> results2,
        double weight1,
        double weight2
    );
}
```

#### 步骤2：实现RankFusionServiceImpl

**文件位置**：`backend/knowledge-base-core/src/main/java/com/company/kb/core/service/impl/RankFusionServiceImpl.java`

```java
package com.company.kb.core.service.impl;

import com.company.kb.core.service.RankFusionService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排序融合服务实现
 * 实现RRF等多种融合算法
 */
@Slf4j
@Service
public class RankFusionServiceImpl implements RankFusionService {

    /**
     * RRF常数默认值
     */
    private static final int DEFAULT_K = 60;

    @Override
    public List<ElasticsearchService.SearchHit> reciprocalRankFusion(
            List<ElasticsearchService.SearchHit> results1,
            List<ElasticsearchService.SearchHit> results2) {

        return reciprocalRankFusion(results1, results2, DEFAULT_K);
    }

    @Override
    public List<ElasticsearchService.SearchHit> reciprocalRankFusion(
            List<ElasticsearchService.SearchHit> results1,
            List<ElasticsearchService.SearchHit> results2,
            int k) {

        log.info("开始RRF融合: results1={}, results2={}, k={}",
            results1.size(), results2.size(), k);

        // 用于存储每个chunk的RRF分数
        Map<String, RRFScore> rrfScores = new HashMap<>();

        // 处理第一个结果列表（BM25）
        for (int i = 0; i < results1.size(); i++) {
            ElasticsearchService.SearchHit hit = results1.get(i);
            String chunkId = getChunkId(hit);

            RRFScore score = rrfScores.computeIfAbsent(chunkId, id -> new RRFScore(id));
            score.addRankScore(k, i + 1); // 排名从1开始

            log.debug("BM25排名 {}: chunkId={}, score={}",
                i + 1, chunkId, score.getTotalScore());
        }

        // 处理第二个结果列表（KNN）
        for (int i = 0; i < results2.size(); i++) {
            ElasticsearchService.SearchHit hit = results2.get(i);
            String chunkId = getChunkId(hit);

            RRFScore score = rrfScores.computeIfAbsent(chunkId, id -> new RRFScore(id));
            score.addRankScore(k, i + 1);

            log.debug("KNN排名 {}: chunkId={}, score={}",
                i + 1, chunkId, score.getTotalScore());
        }

        // 按RRF分数降序排序
        List<ElasticsearchService.SearchHit> fusedResults = rrfScores.values().stream()
            .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
            .limit(results1.size() + results2.size()) // 限制结果数量
            .map(rrfScore -> {
                // 查找原始SearchHit并设置RRF分数
                ElasticsearchService.SearchHit hit = findByChunkId(results1, results2, rrfScore.chunkId);
                if (hit != null) {
                    hit.setRrfScore(rrfScore.getTotalScore());
                }
                return hit;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info("RRF融合完成: fusedResults={}, topScore={}",
            fusedResults.size(),
            fusedResults.isEmpty() ? 0 : fusedResults.get(0).getRrfScore());

        return fusedResults;
    }

    @Override
    public List<ElasticsearchService.SearchHit> weightedFusion(
            List<ElasticsearchService.SearchHit> results1,
            List<ElasticsearchService.SearchHit> results2,
            double weight1,
            double weight2) {

        log.info("开始加权融合: results1={}, results2={}, weight1={}, weight2={}",
            results1.size(), results2.size(), weight1, weight2);

        // 归一化分数到0-1范围
        double maxScore1 = results1.stream()
            .map(ElasticsearchService.SearchHit::getScore)
            .max(Double::compare)
            .orElse(1.0);

        double maxScore2 = results2.stream()
            .map(ElasticsearchService.SearchHit::getScore)
            .max(Double::compare)
            .orElse(1.0);

        // 用于存储加权分数
        Map<String, WeightedScore> weightedScores = new HashMap<>();

        // 处理第一个结果列表
        for (ElasticsearchService.SearchHit hit : results1) {
            String chunkId = getChunkId(hit);
            double normalizedScore = hit.getScore() / maxScore1;

            WeightedScore score = weightedScores.computeIfAbsent(
                chunkId, id -> new WeightedScore(id));
            score.addScore(normalizedScore * weight1);
        }

        // 处理第二个结果列表
        for (ElasticsearchService.SearchHit hit : results2) {
            String chunkId = getChunkId(hit);
            double normalizedScore = hit.getScore() / maxScore2;

            WeightedScore score = weightedScores.computeIfAbsent(
                chunkId, id -> new WeightedScore(id));
            score.addScore(normalizedScore * weight2);
        }

        // 按加权分数降序排序
        List<ElasticsearchService.SearchHit> fusedResults = weightedScores.values().stream()
            .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
            .map(weightedScore -> {
                ElasticsearchService.SearchHit hit = findByChunkId(results1, results2, weightedScore.chunkId);
                if (hit != null) {
                    hit.setRrfScore(weightedScore.getTotalScore());
                }
                return hit;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info("加权融合完成: fusedResults={}", fusedResults.size());

        return fusedResults;
    }

    /**
     * 获取chunk的唯一标识
     */
    private String getChunkId(ElasticsearchService.SearchHit hit) {
        // 使用documentId和chunkId组合作为唯一标识
        return hit.getDocumentId() + "_" + hit.getChunkId();
    }

    /**
     * 根据chunkId查找SearchHit
     */
    private ElasticsearchService.SearchHit findByChunkId(
            List<ElasticsearchService.SearchHit> results1,
            List<ElasticsearchService.SearchHit> results2,
            String chunkId) {

        for (ElasticsearchService.SearchHit hit : results1) {
            if (getChunkId(hit).equals(chunkId)) {
                return hit;
            }
        }

        for (ElasticsearchService.SearchHit hit : results2) {
            if (getChunkId(hit).equals(chunkId)) {
                return hit;
            }
        }

        return null;
    }

    /**
     * RRF分数内部类
     */
    private static class RRFScore {
        private final String chunkId;
        private double totalScore = 0.0;

        public RRFScore(String chunkId) {
            this.chunkId = chunkId;
        }

        public void addRankScore(int k, int rank) {
            totalScore += (double) k / (k + rank);
        }

        public double getTotalScore() {
            return totalScore;
        }

        public String getChunkId() {
            return chunkId;
        }
    }

    /**
     * 加权分数内部类
     */
    private static class WeightedScore {
        private final String chunkId;
        private double totalScore = 0.0;

        public WeightedScore(String chunkId) {
            this.chunkId = chunkId;
        }

        public void addScore(double score) {
            totalScore += score;
        }

        public double getTotalScore() {
            return totalScore;
        }

        public String getChunkId() {
            return chunkId;
        }
    }
}
```

#### 步骤3：在ElasticsearchService.SearchHit中添加rrfScore字段

**文件位置**：`backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/elasticsearch/service/ElasticsearchService.java`

```java
public static class SearchHit {
    private Long id;
    private Long documentId;
    private Long chunkId;
    private String title;
    private String fileName;
    private String content;
    private String fileType;
    private Double score;           // 原始分数
    private Double rrfScore;        // RRF融合分数（新增）
    private Integer position;
    private String uploadedBy;
    private Boolean isPublic;

    // 添加getter和setter
    public Double getRrfScore() {
        return rrfScore;
    }

    public void setRrfScore(Double rrfScore) {
        this.rrfScore = rrfScore;
    }
}
```

### 测试验证

```java
@Test
public void testRRF() {
    // 创建测试数据
    List<SearchHit> bm25Results = Arrays.asList(
        createHit(1L, 1L, 0.9, "doc1"),
        createHit(2L, 2L, 0.8, "doc2"),
        createHit(3L, 3L, 0.7, "doc3")
    );

    List<SearchHit> knnResults = Arrays.asList(
        createHit(2L, 2L, 0.95, "doc2"),  // 同一个文档
        createHit(4L, 4L, 0.85, "doc4"),
        createHit(1L, 1L, 0.75, "doc1")   // 同一个文档
    );

    // 执行RRF融合
    List<SearchHit> fused = rankFusionService.reciprocalRankFusion(bm25Results, knnResults);

    // 验证结果
    // doc2应该排第一（在两个列表中都靠前）
    // doc1应该排第二（在两个列表中都出现）
    // doc4应该排第三（只在KNN中出现）
    // doc3应该排第四（只在BM25中出现）

    assertEquals(4, fused.size());
    assertEquals("doc2", fused.get(0).getTitle());
    assertTrue(fused.get(0).getRrfScore() > fused.get(1).getRrfScore());
}
```

---

## 模块二：后端 - 混合搜索API优化

### 任务说明

修改`/search/hybrid`端点，使用RRF算法返回融合后的统一结果列表。

### 实现步骤

**文件位置**：`backend/knowledge-base-api/src/main/java/com/company/kb/controller/SearchController.java`

```java
package com.company.kb.controller;

import com.company.kb.common.result.Result;
import com.company.kb.core.service.VectorEmbeddingService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
     * @return 融合后的搜索结果
     */
    @PostMapping("/hybrid")
    public Result<HybridSearchResponse> searchHybrid(
            @RequestBody SearchRequest request) {

        try {
            log.info("混合搜索: query={}", request.getQuery());

            // 1. 生成查询向量
            float[] queryVector = vectorEmbeddingService.generateEmbedding(request.getQuery());

            // 2. 并行执行BM25和KNN搜索
            java.util.concurrent.CompletableFuture<List<ElasticsearchService.SearchHit>> bm25Future =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return elasticsearchService.searchByBM25(request.getQuery(), null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            java.util.concurrent.CompletableFuture<List<ElasticsearchService.SearchHit>> knnFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return elasticsearchService.searchByKNN(queryVector, null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            // 等待两个搜索完成
            java.util.concurrent.CompletableFuture.allOf(bm25Future, knnFuture).join();

            List<ElasticsearchService.SearchHit> bm25Results = bm25Future.join();
            List<ElasticsearchService.SearchHit> knnResults = knnFuture.join();

            log.info("BM25结果数: {}, KNN结果数: {}", bm25Results.size(), knnResults.size());

            // 3. RRF融合排序
            List<ElasticsearchService.SearchHit> fusedResults =
                rankFusionService.reciprocalRankFusion(bm25Results, knnResults);

            // 4. 应用返回数量限制
            int size = request.getSize() != null && request.getSize() > 0
                ? request.getSize()
                : 10;

            List<ElasticsearchService.SearchHit> finalResults = fusedResults.stream()
                .limit(size)
                .collect(java.util.stream.Collectors.toList());

            // 5. 构建响应
            HybridSearchResponse response = new HybridSearchResponse();
            response.setResults(finalResults);
            response.setTotal(finalResults.size());
            response.setQuery(request.getQuery());
            response.setBm25Count(bm25Results.size());
            response.setKnnCount(knnResults.size());

            log.info("混合搜索完成: totalResults={}, topScore={}",
                finalResults.size(),
                finalResults.isEmpty() ? 0 : finalResults.get(0).getRrfScore());

            return Result.success(response);

        } catch (Exception e) {
            log.error("混合搜索失败", e);
            return Result.failed(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索请求DTO
     */
    public static class SearchRequest {
        private String query;
        private Integer size;
        // TODO: 添加筛选参数
        // private List<String> departments;
        // private List<String> tags;
        // private List<String> fileTypes;
        // private String dateFrom;
        // private String dateTo;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    /**
     * 混合搜索响应DTO
     */
    public static class HybridSearchResponse {
        private List<ElasticsearchService.SearchHit> results;
        private Integer total;
        private String query;
        private Integer bm25Count;
        private Integer knnCount;

        // Getters and Setters
        public List<ElasticsearchService.SearchHit> getResults() { return results; }
        public void setResults(List<ElasticsearchService.SearchHit> results) { this.results = results; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getBm25Count() { return bm25Count; }
        public void setBm25Count(Integer bm25Count) { this.bm25Count = bm25Count; }
        public Integer getKnnCount() { return knnCount; }
        public void setKnnCount(Integer knnCount) { this.knnCount = knnCount; }
    }
}
```

### 依赖注入

在`SearchController`中添加`RankFusionService`注入：

```java
private final RankFusionService rankFusionService;
```

---

## 模块三：前端 - searchService.ts

### 任务说明

创建前端搜索服务，封装所有搜索相关的API调用。

### 实现步骤

**文件位置**：`frontend/src/services/searchService.ts`

```typescript
import api from './api'

/**
 * 搜索服务
 * 提供BM25、KNN、混合搜索等API
 */

// 搜索结果类型
export interface SearchChunk {
  id: number
  documentId: number
  chunkId: number
  title: string
  fileName: string
  content: string
  fileType: string
  score: number
  rrfScore?: number  // RRF融合分数
  position: number
  uploadedBy?: string
  isPublic: boolean
  highlight?: {
    title?: string[]
    content?: string[]
  }
}

// 混合搜索响应类型
export interface HybridSearchResponse {
  results: SearchChunk[]
  total: number
  query: string
  bm25Count: number
  knnCount: number
}

// 搜索请求类型
export interface SearchRequest {
  query: string
  size?: number
  // TODO: 添加筛选参数
  // departments?: string[]
  // tags?: string[]
  // fileTypes?: string[]
  // dateFrom?: string
  // dateTo?: string
}

const searchService = {
  /**
   * BM25关键词搜索
   */
  searchByBM25: async (request: SearchRequest): Promise<SearchChunk[]> => {
    const response = await api.post('/search/bm25', request)
    return response.data || []
  },

  /**
   * KNN向量搜索
   */
  searchByKNN: async (request: SearchRequest): Promise<SearchChunk[]> => {
    const response = await api.post('/search/knn', request)
    return response.data || []
  },

  /**
   * 混合搜索（推荐使用）
   */
  searchHybrid: async (request: SearchRequest): Promise<HybridSearchResponse> => {
    const response = await api.post('/search/hybrid', request)
    return response.data
  },

  /**
   * 智能搜索（默认使用混合搜索）
   */
  search: async (
    query: string,
    options?: {
      size?: number
      method?: 'bm25' | 'knn' | 'hybrid'
    }
  ): Promise<HybridSearchResponse> => {
    const method = options?.method || 'hybrid'

    if (method === 'hybrid') {
      return searchService.searchHybrid({
        query,
        size: options?.size || 10
      })
    } else if (method === 'bm25') {
      const results = await searchService.searchByBM25({
        query,
        size: options?.size || 10
      })
      return {
        results,
        total: results.length,
        query,
        bm25Count: results.length,
        knnCount: 0
      }
    } else {
      const results = await searchService.searchByKNN({
        query,
        size: options?.size || 10
      })
      return {
        results,
        total: results.length,
        query,
        bm25Count: 0,
        knnCount: results.length
      }
    }
  }
}

export default searchService
```

---

## 模块四：前端 - 搜索结果展示

### 任务说明

更新`SearchPage.tsx`，使用混合搜索API，正确展示chunk级别的搜索结果。

### 实现步骤

**文件位置**：`frontend/src/pages/SearchPage.tsx`

```typescript
import { useState } from 'react'
import {
  Card, Input, Button, Space, Typography, List, Tag,
  Divider, message, Empty, Spin, Tooltip, Collapse
} from 'antd'
import {
  SearchOutlined, FilterOutlined, FileTextOutlined,
  InfoCircleOutlined, CopyOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import searchService from '../services/searchService'

const { Title, Text, Paragraph } = Typography
const { Search } = Input
const { Panel } = Collapse

// 搜索结果类型
interface SearchChunk {
  id: number
  documentId: number
  chunkId: number
  title: string
  fileName: string
  content: string
  fileType: string
  score: number
  rrfScore?: number
  position: number
  uploadedBy?: string
  isPublic: boolean
}

// 搜索响应类型
interface SearchResponse {
  results: SearchChunk[]
  total: number
  query: string
  bm25Count: number
  knnCount: number
}

// 搜索提示词
const SEARCH_PLACEHOLDER = '搜索文档内容...'
const EMPTY_HELP = (
  <div>
    <Paragraph>未找到相关文档</Paragraph>
    <Divider />
    <Text type="secondary">
      💡 搜索建议：
      <br />• 尝试更换关键词
      <br />• 使用更简短的关键词
      <br />• 搜索文档的核心内容
    </Text>
  </div>
)

const SearchPage = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [searching, setSearching] = useState(false)
  const [results, setResults] = useState<SearchResponse | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)

  const handleSearch = async (query: string, pageNum: number = 1) => {
    if (!query.trim()) {
      message.warning('请输入搜索关键词')
      return
    }

    try {
      setSearching(true)

      // 使用混合搜索API
      const response = await searchService.searchHybrid({
        query: query.trim(),
        size: pageSize
      })

      setResults(response)
      setPage(pageNum)

      console.log('搜索结果:', response)
    } catch (error: any) {
      message.error('搜索失败: ' + (error.response?.data?.message || error.message))
      setResults(null)
    } finally {
      setSearching(false)
    }
  }

  const handleDocumentClick = (documentId: number, chunkId: number) => {
    // 跳转到文档详情页，并定位到相关chunk
    navigate(`/documents/${documentId}?chunk=${chunkId}`)
  }

  const handleCopyContent = (content: string, e: React.MouseEvent) => {
    e.stopPropagation()
    navigator.clipboard.writeText(content)
    message.success('内容已复制')
  }

  const highlightText = (text: string, query: string) => {
    if (!query.trim()) return text

    const regex = new RegExp(`(${query.split(/\s+/).join('|')})`, 'gi')
    const parts = text.split(regex)

    return parts.map((part, index) =>
      regex.test(part) ? (
        <mark key={index} style={{ backgroundColor: '#ffd666', padding: '0 2px' }}>
          {part}
        </mark>
      ) : (
        <span key={index}>{part}</span>
      )
    )
  }

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* 页面标题 */}
      <Title level={2}>🔍 智能搜索</Title>
      <Text type="secondary">
        使用混合搜索（关键词+语义）快速找到相关文档内容
      </Text>

      {/* 搜索框 */}
      <Card style={{ marginTop: '16px' }}>
        <Space.Compact style={{ width: '100%' }}>
          <Search
            placeholder={SEARCH_PLACEHOLDER}
            enterButton={<SearchOutlined />}
            size="large"
            loading={searching}
            onSearch={(value) => handleSearch(value)}
            onChange={(e) => setSearchQuery(e.target.value)}
            maxLength={200}
            autoFocus
          />
          <Button
            icon={<FilterOutlined />}
            onClick={() => message.info('高级筛选功能开发中...')}
          >
            筛选
          </Button>
        </Space.Compact>

        {/* 搜索提示 */}
        {!results && !searching && (
          <div style={{ marginTop: '16px', padding: '12px', background: '#f0f2f5', borderRadius: '4px' }}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text strong>💡 搜索提示</Text>
              <Text type="secondary">• 支持关键词搜索和语义搜索</Text>
              <Text type="secondary">• 可以搜索文档标题和内容</Text>
              <Text type="secondary">• 尝试搜索："报销流程"、"请假制度"等</Text>
            </Space>
          </div>
        )}
      </Card>

      {/* 搜索结果 */}
      {results && (
        <Card style={{ marginTop: '16px' }}>
          {/* 结果统计 */}
          <div style={{ marginBottom: '16px' }}>
            <Space split={<Divider type="vertical" />}>
              <Text>
                找到 <Text strong>{results.total}</Text> 个相关片段
              </Text>
              <Text type="secondary">
                BM25: {results.bm25Count} | KNN: {results.knnCount}
              </Text>
            </Space>
          </div>

          {/* 结果列表 */}
          <List
            itemLayout="vertical"
            size="large"
            pagination={false}
            dataSource={results.results}
            renderItem={(item, index) => (
              <List.Item
                key={item.id}
                style={{
                  cursor: 'pointer',
                  padding: '16px',
                  marginBottom: index < results.results.length - 1 ? '16px' : 0,
                  background: '#fafafa',
                  borderRadius: '8px',
                  border: '1px solid #f0f0f0'
                }}
                onClick={() => handleDocumentClick(item.documentId, item.chunkId)}
              >
                {/* 标题和分数 */}
                <div style={{ marginBottom: '8px' }}>
                  <Space>
                    <Text strong style={{ fontSize: '15px' }}>
                      {index + 1}. {highlightText(item.title, searchQuery)}
                    </Text>
                    <Tag color="blue">{item.fileType.toUpperCase()}</Tag>
                    {item.isPublic && <Tag color="green">公开</Tag>}
                    <Tooltip title="相关度分数（RRF融合）">
                      <Tag color="orange">
                        {(item.rrfScore ? item.rrfScore : item.score * 100).toFixed(1)}
                      </Tag>
                    </Tooltip>
                    <Tooltip title="复制内容">
                      <Button
                        type="text"
                        size="small"
                        icon={<CopyOutlined />}
                        onClick={(e) => handleCopyContent(item.content, e)}
                      />
                    </Tooltip>
                  </Space>
                </div>

                {/* 文档信息 */}
                <div style={{ marginBottom: '8px' }}>
                  <Space split={<Divider type="vertical" />}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      文件: {item.fileName}
                    </Text>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      片段: #{item.chunkId + 1}
                    </Text>
                  </Space>
                </div>

                {/* 内容预览 */}
                <Paragraph
                  ellipsis={{ rows: 3, expandable: true, symbol: '展开' }}
                  style={{
                    marginBottom: 0,
                    padding: '8px',
                    background: '#fff',
                    borderRadius: '4px'
                  }}
                >
                  {highlightText(item.content, searchQuery)}
                </Paragraph>
              </List.Item>
            )}
          />

          {/* 分页 */}
          {results.total > pageSize && (
            <div style={{ marginTop: '24px', textAlign: 'center' }}>
              <Space>
                {page > 1 && (
                  <Button onClick={() => handleSearch(searchQuery, page - 1)}>
                    上一页
                  </Button>
                )}
                <Text>第 {page} 页</Text>
                {page * pageSize < results.total && (
                  <Button type="primary" onClick={() => handleSearch(searchQuery, page + 1)}>
                    下一页
                  </Button>
                )}
              </Space>
            </div>
          )}
        </Card>
      )}

      {/* 搜索中状态 */}
      {searching && (
        <Card style={{ marginTop: '16px' }}>
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" />
            <div style={{ marginTop: '16px' }}>
              <Text type="secondary">正在智能检索中...</Text>
            </div>
          </div>
        </Card>
      )}

      {/* 空状态 */}
      {!searching && results && results.total === 0 && (
        <Card style={{ marginTop: '16px' }}>
          <Empty description={EMPTY_HELP} />
        </Card>
      )}
    </div>
  )
}

export default SearchPage
```

---

## 开发检查清单

### 后端检查项

- [ ] RankFusionService接口创建完成
- [ ] RankFusionServiceImpl实现完成
- [ ] RRF算法单元测试通过
- [ ] SearchHit添加rrfScore字段
- [ ] SearchController更新/hybrid端点
- [ ] 混合搜索返回融合结果
- [ ] API测试通过（curl/Postman）

### 前端检查项

- [ ] searchService.ts创建完成
- [ ] SearchPage.tsx更新完成
- [ ] 调用混合搜索API
- [ ] 正确展示chunk结果
- [ ] 搜索高亮功能实现
- [ ] 分页功能正常
- [ ] 空状态正常显示
- [ ] 错误处理完善

### 联调检查项

- [ ] 前后端联调成功
- [ ] 搜索结果正确排序
- [ ] RRF分数正确显示
- [ ] 点击跳转文档详情正常
- [ ] 边界条件处理（空结果、网络错误等）

---

## API测试用例

### 混合搜索测试

```bash
# 测试混合搜索
curl -X POST http://localhost:8080/search/hybrid \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "query": "报销流程",
    "size": 10
  }'

# 预期响应
{
  "code": 200,
  "data": {
    "results": [
      {
        "id": 123,
        "documentId": 10,
        "chunkId": 2,
        "title": "差旅费报销管理办法",
        "fileName": "差旅费报销制度.pdf",
        "content": "员工出差后需在7个工作日内完成报销申请...",
        "score": 0.85,
        "rrfScore": 1.5,
        ...
      }
    ],
    "total": 10,
    "query": "报销流程",
    "bm25Count": 15,
    "knnCount": 12
  }
}
```

---

## 常见问题

### Q1: RRF分数计算不正确？

**检查**：
- rank是否从1开始（不是0）
- k值是否设置为60
- chunkId是否正确计算

### Q2: 前端搜索无结果？

**检查**：
- 后端API是否正常返回
- CORS配置是否正确
- 请求体格式是否正确

### Q3: 搜索结果排序不理想？

**调整**：
- 尝试不同的k值（30-100）
- 考虑添加文档级别boost
- 检查向量 embedding 质量

---

*文档版本：1.0*
*创建日期：2026-04-10*
*作者：Knowledge Base Development Team*
