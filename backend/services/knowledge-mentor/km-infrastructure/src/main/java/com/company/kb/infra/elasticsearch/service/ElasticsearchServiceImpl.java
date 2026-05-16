package com.company.kb.infra.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.company.kb.infra.elasticsearch.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch服务实现
 * 实现BM25、KNN、批量索引等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient client;

    @Value("${elasticsearch.index-name:kb_document_chunks}")
    private String indexName;

    @Override
    public void createIndex(String indexName) throws Exception {
        if (indexExists(indexName)) {
            log.info("索引已存在，跳过创建: {}", indexName);
            return;
        }

        CreateIndexRequest createRequest = new CreateIndexRequest.Builder()
            .index(indexName)
            .settings(s -> s
                .numberOfShards("3")
                .numberOfReplicas("1")
            )
            .mappings(m -> m
                .properties("content", p -> p
                    .text(t -> t
                        .analyzer("ik_max_word")  // 使用ik_max_word进行索引时分词
                        .searchAnalyzer("ik_smart")  // 使用ik_smart进行搜索时分词
                        .fields("keyword", f -> f.keyword(k -> k))
                    )
                )
                .properties("contentVector", p -> p
                    .denseVector(dv -> dv
                        .dims(1024)
                        .index(true)
                        .similarity("cosine")
                    )
                )
                .properties("documentId", p -> p.long_(l -> l))
                .properties("chunkId", p -> p.long_(l -> l))
                .properties("title", p -> p
                    .text(t -> t
                        .analyzer("ik_max_word")
                        .searchAnalyzer("ik_smart")
                    )
                )
                .properties("fileName", p -> p.keyword(k -> k))
                .properties("fileType", p -> p.keyword(k -> k))
                .properties("department", p -> p.keyword(k -> k))
                .properties("tags", p -> p.keyword(k -> k))
                .properties("uploadedBy", p -> p.long_(l -> l))
                .properties("isPublic", p -> p.boolean_(b -> b))
                .properties("createdAt", p -> p.date(d -> d))
            )
            .build();

        client.indices().create(createRequest);
        log.info("索引创建成功: {}", indexName);
    }

    @Override
    public void deleteIndex(String indexName) throws Exception {
        if (!indexExists(indexName)) {
            log.warn("索引不存在，跳过删除: {}", indexName);
            return;
        }

        client.indices().delete(d -> d.index(indexName));
        log.info("索引删除成功: {}", indexName);
    }

    @Override
    public boolean indexExists(String indexName) throws Exception {
        ExistsRequest existsRequest = new ExistsRequest.Builder()
            .index(indexName)
            .build();

        return client.indices().exists(existsRequest).value();
    }

    @Override
    public List<SearchHit> searchByBM25(String query, List<Long> accessibleDocIds) throws Exception {
        // 构建布尔查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // BM25查询 - 对content字段使用multi_match查询，支持高亮
        boolBuilder.must(m -> m
            .multiMatch(mm -> mm
                .query(query)  // multiMatch.query()直接接受String
                .fields("content", "title")  // 搜索content和title字段
                .type(TextQueryType.BestFields)
            )
        );

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("documentId")
                    .terms(tv -> tv
                        .value(accessibleDocIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 添加highlight配置
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(boolBuilder.build()._toQuery())
            .highlight(h -> h
                .fields("content", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(150)  // 每个片段150字符
                    .numberOfFragments(3)  // 返回最多3个片段
                )
                .fields("title", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(100)
                    .numberOfFragments(1)
                )
            )
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                    searchHit.setChunkIndex(hit.source().getPosition());
                }

                // 处理高亮结果
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    // 优先使用content字段的高亮，如果没有则使用title的高亮
                    var contentHighlights = hit.highlight().get("content");
                    var titleHighlights = hit.highlight().get("title");

                    if (contentHighlights != null && !contentHighlights.isEmpty()) {
                        // 合并多个content高亮片段
                        String combinedHighlights = String.join(" ... ", contentHighlights);
                        searchHit.setHighlight(combinedHighlights);
                    } else if (titleHighlights != null && !titleHighlights.isEmpty()) {
                        searchHit.setHighlight(titleHighlights.get(0));
                    }
                }

                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByKNN(float[] queryVector, List<Long> accessibleDocIds) throws Exception {
        // 构建KNN查询 - 使用scriptScore作为KNN的替代方案
        // 在ES 8.x中，KNN查询通过knn参数或nearest API使用，这里使用functionScore结合向量相似度

        // 先使用match_all作为基础查询
        Query baseQuery = Query.of(q -> q.matchAll(m -> m));

        // 如果有权限限制，添加过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            baseQuery = Query.of(q -> q
                .bool(b -> b
                    .must(m -> m.matchAll(ma -> ma))
                    .filter(f -> f
                        .terms(t -> t
                            .field("documentId")
                            .terms(tv -> tv
                                .value(accessibleDocIds.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList()))
                            )
                        )
                    )
                )
            );
        }

        // 使用KNN参数而不是query DSL中的knn
        List<Float> queryVectorList = new ArrayList<>();
        for (float v : queryVector) {
            queryVectorList.add(v);
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(baseQuery)
            .knn(k -> k
                .field("contentVector")
                .queryVector(queryVectorList)
                .k(20)
                .numCandidates(100)
            )
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                }
                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByBM25(
            String query,
            List<Long> accessibleDocIds,
            List<String> departments,
            List<String> tags,
            List<String> fileTypes,
            String dateFrom,
            String dateTo
    ) throws Exception {
        // 构建布尔查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // BM25查询
        boolBuilder.must(m -> m
            .multiMatch(mm -> mm
                .query(query)  // multiMatch.query()直接接受String
                .fields("content", "title")
                .type(TextQueryType.BestFields)
            )
        );

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("documentId")
                    .terms(tv -> tv
                        .value(accessibleDocIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 部门筛选
        if (departments != null && !departments.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("department")
                    .terms(tv -> tv
                        .value(departments.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 标签筛选
        if (tags != null && !tags.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("tags")
                    .terms(tv -> tv
                        .value(tags.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 文件类型筛选
        if (fileTypes != null && !fileTypes.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("file_type")
                    .terms(tv -> tv
                        .value(fileTypes.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 时间范围筛选
        if (dateFrom != null || dateTo != null) {
            var rangeQuery = new co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder();
            rangeQuery.field("created_at");

            if (dateFrom != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(dateFrom));
            }
            if (dateTo != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(dateTo));
            }

            boolBuilder.filter(f -> f.range(rangeQuery.build()));
        }

        // 添加highlight配置
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(boolBuilder.build()._toQuery())
            .highlight(h -> h
                .fields("content", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(150)
                    .numberOfFragments(3)
                )
                .fields("title", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(100)
                    .numberOfFragments(1)
                )
            )
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                    searchHit.setChunkIndex(hit.source().getPosition());
                }

                // 处理高亮结果
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    var contentHighlights = hit.highlight().get("content");
                    var titleHighlights = hit.highlight().get("title");

                    if (contentHighlights != null && !contentHighlights.isEmpty()) {
                        String combinedHighlights = String.join(" ... ", contentHighlights);
                        searchHit.setHighlight(combinedHighlights);
                    } else if (titleHighlights != null && !titleHighlights.isEmpty()) {
                        searchHit.setHighlight(titleHighlights.get(0));
                    }
                }

                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByKNN(
            float[] queryVector,
            List<Long> accessibleDocIds,
            List<String> departments,
            List<String> tags,
            List<String> fileTypes,
            String dateFrom,
            String dateTo
    ) throws Exception {
        // 先使用match_all作为基础查询
        Query baseQuery = Query.of(q -> q.matchAll(m -> m));

        // 构建布尔查询添加筛选条件
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.must(m -> m.matchAll(ma -> ma));

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("documentId")
                    .terms(tv -> tv
                        .value(accessibleDocIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 部门筛选
        if (departments != null && !departments.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("department")
                    .terms(tv -> tv
                        .value(departments.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 标签筛选
        if (tags != null && !tags.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("tags")
                    .terms(tv -> tv
                        .value(tags.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 文件类型筛选
        if (fileTypes != null && !fileTypes.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("file_type")
                    .terms(tv -> tv
                        .value(fileTypes.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 时间范围筛选
        if (dateFrom != null || dateTo != null) {
            var rangeQuery = new co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder();
            rangeQuery.field("created_at");

            if (dateFrom != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(dateFrom));
            }
            if (dateTo != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(dateTo));
            }

            boolBuilder.filter(f -> f.range(rangeQuery.build()));
        }

        // 使用KNN参数
        List<Float> queryVectorList = new ArrayList<>();
        for (float v : queryVector) {
            queryVectorList.add(v);
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(boolBuilder.build()._toQuery())
            .knn(k -> k
                .field("contentVector")
                .queryVector(queryVectorList)
                .k(20)
                .numCandidates(100)
            )
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                }
                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByBM25(
            String query,
            List<Long> accessibleDocIds,
            List<String> departments,
            List<String> tags,
            List<String> fileTypes,
            String dateFrom,
            String dateTo,
            int from,
            int size
    ) throws Exception {
        // 构建布尔查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // BM25查询
        boolBuilder.must(m -> m
            .multiMatch(mm -> mm
                .query(query)  // multiMatch.query()直接接受String
                .fields("content", "title")
                .type(TextQueryType.BestFields)
            )
        );

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("documentId")
                    .terms(tv -> tv
                        .value(accessibleDocIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 部门筛选
        if (departments != null && !departments.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("department")
                    .terms(tv -> tv
                        .value(departments.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 标签筛选
        if (tags != null && !tags.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("tags")
                    .terms(tv -> tv
                        .value(tags.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 文件类型筛选
        if (fileTypes != null && !fileTypes.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("file_type")
                    .terms(tv -> tv
                        .value(fileTypes.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 时间范围筛选
        if (dateFrom != null || dateTo != null) {
            var rangeQuery = new co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder();
            rangeQuery.field("created_at");

            if (dateFrom != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(dateFrom));
            }
            if (dateTo != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(dateTo));
            }

            boolBuilder.filter(f -> f.range(rangeQuery.build()));
        }

        // 添加highlight配置和分页
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .from(from)
            .size(size)
            .query(boolBuilder.build()._toQuery())
            .highlight(h -> h
                .fields("content", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(150)
                    .numberOfFragments(3)
                )
                .fields("title", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(100)
                    .numberOfFragments(1)
                )
            )
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                    searchHit.setChunkIndex(hit.source().getPosition());
                }

                // 处理高亮结果
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    var contentHighlights = hit.highlight().get("content");
                    var titleHighlights = hit.highlight().get("title");

                    if (contentHighlights != null && !contentHighlights.isEmpty()) {
                        String combinedHighlights = String.join(" ... ", contentHighlights);
                        searchHit.setHighlight(combinedHighlights);
                    } else if (titleHighlights != null && !titleHighlights.isEmpty()) {
                        searchHit.setHighlight(titleHighlights.get(0));
                    }
                }

                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByKNN(
            float[] queryVector,
            List<Long> accessibleDocIds,
            List<String> departments,
            List<String> tags,
            List<String> fileTypes,
            String dateFrom,
            String dateTo,
            int from,
            int size
    ) throws Exception {
        // 先使用match_all作为基础查询
        Query baseQuery = Query.of(q -> q.matchAll(m -> m));

        // 构建布尔查询添加筛选条件
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.must(m -> m.matchAll(ma -> ma));

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("documentId")
                    .terms(tv -> tv
                        .value(accessibleDocIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 部门筛选
        if (departments != null && !departments.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("department")
                    .terms(tv -> tv
                        .value(departments.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 标签筛选
        if (tags != null && !tags.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("tags")
                    .terms(tv -> tv
                        .value(tags.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 文件类型筛选
        if (fileTypes != null && !fileTypes.isEmpty()) {
            boolBuilder.filter(f -> f
                .terms(t -> t
                    .field("file_type")
                    .terms(tv -> tv
                        .value(fileTypes.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList()))
                    )
                )
            );
        }

        // 时间范围筛选
        if (dateFrom != null || dateTo != null) {
            var rangeQuery = new co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder();
            rangeQuery.field("created_at");

            if (dateFrom != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(dateFrom));
            }
            if (dateTo != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(dateTo));
            }

            boolBuilder.filter(f -> f.range(rangeQuery.build()));
        }

        // 使用KNN参数 - 提高精度
        List<Float> queryVectorList = new ArrayList<>();
        for (float v : queryVector) {
            queryVectorList.add(v);
        }

        // 严格调整KNN参数以提高精度（减少语义搜索的干扰）
        int knnK = Math.min(5, size);  // 大幅减少k值，只返回最相似的5个
        int numCandidates = Math.min(10, knnK * 2);  // 减少候选数量，提高精确度

        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .from(from)
            .size(size)
            .query(baseQuery)
            .knn(k -> k
                .field("contentVector")
                .queryVector(queryVectorList)
                .k(knnK)
                .numCandidates(numCandidates)
            )
            .minScore(0.85)  // 提高相似度阈值到0.85，过滤掉低相似度结果
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                SearchHit searchHit = new SearchHit();
                searchHit.setId(hit.id());
                searchHit.setScore(hit.score() != null ? hit.score() : 0.0);
                if (hit.source() != null) {
                    searchHit.setContent(hit.source().getContent());
                    searchHit.setTitle(hit.source().getTitle());
                    searchHit.setFileName(hit.source().getFileName());
                    searchHit.setDocumentId(hit.source().getDocumentId());
                }
                return searchHit;
            })
            .collect(Collectors.toList());
    }

    @Override
    public void bulkIndexChunks(List<DocumentChunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<BulkOperation> operations = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            chunk.setCreatedAt(System.currentTimeMillis());

            operations.add(BulkOperation.of(b -> b
                .index(i -> i
                    .index(indexName)
                    .id(chunk.getDocumentId() + "_" + chunk.getChunkId())
                    .document(chunk)
                )
            ));
        }

        BulkRequest bulkRequest = new BulkRequest.Builder()
            .operations(operations)
            .build();

        BulkResponse bulkResponse = client.bulk(bulkRequest);

        if (bulkResponse.errors()) {
            log.error("批量索引存在错误");
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("索引失败: id={}, error={}", item.id(), item.error().reason());
                }
            });
        } else {
            log.info("批量索引成功: count={}", chunks.size());
        }
    }

    @Override
    public void deleteDocumentChunks(Long documentId) throws Exception {
        // 删除指定文档的所有块
        DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest.Builder()
            .index(indexName)
            .query(q -> q
                .term(t -> t
                    .field("documentId")
                    .value(documentId)  // term.value()直接接受Long
                )
            )
            .build();

        client.deleteByQuery(deleteRequest);
        log.info("删除文档块: documentId={}", documentId);
    }

    @Override
    public long countDocuments() throws Exception {
        CountRequest countRequest = new CountRequest.Builder()
            .index(indexName)
            .build();

        CountResponse countResponse = client.count(countRequest);
        long count = countResponse.count();
        log.debug("索引文档数量: index={}, count={}", indexName, count);
        return count;
    }
}
