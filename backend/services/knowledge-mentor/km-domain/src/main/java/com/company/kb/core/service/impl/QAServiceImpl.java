package com.company.kb.core.service.impl;

import com.company.kb.core.service.ChatService;
import com.company.kb.core.service.ChunkInfo;
import com.company.kb.core.service.QAService;
import com.company.kb.core.service.VectorEmbeddingService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Q&A service implementation with RAG pipeline
 * Integrates with Elasticsearch for hybrid search (BM25 + KNN)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAServiceImpl implements QAService {

    private final VectorEmbeddingService vectorEmbeddingService;
    private final ChatService chatService;
    private final ElasticsearchService elasticsearchService;

    @Override
    public AnswerDTO processQuery(String question, String userId) throws Exception {
        log.info("Processing query from user {}: {}", userId, question);

        // 1. Generate query vector
        float[] queryVector = vectorEmbeddingService.generateEmbedding(question);
        log.debug("Query vector generated, dimensions: {}", queryVector.length);

        // 2. Search relevant chunks via Elasticsearch (hybrid BM25 + KNN)
        List<ChunkInfo> chunks = retrieveRelevantChunks(question, userId);
        log.info("Found {} relevant chunks", chunks.size());

        // 3. Build context
        String context = buildContext(chunks);

        // 4. Generate answer using AI
        String answer = chatService.generateAnswer(question, context);

        // 5. Build response
        AnswerDTO response = AnswerDTO.builder()
                .answer(answer)
                .sources(buildSources(chunks))
                .citations(new ArrayList<>())
                .build();

        return response;
    }

    @Override
    public void processQueryWithStream(String question, String userId, StreamCallback callback) throws Exception {
        log.info("Processing streaming query from user {}: {}", userId, question);

        // 1. Retrieve relevant chunks
        List<ChunkInfo> chunks = retrieveRelevantChunks(question, userId);
        log.info("Found {} relevant chunks", chunks.size());

        // 2. Build context
        String context = buildContext(chunks);

        // 3. Stream answer using AI
        chatService.streamAnswer(question, context, new ChatService.StreamCallback() {
            @Override
            public void onToken(String token) {
                callback.onToken(token);
            }

            @Override
            public void onComplete() {
                callback.onComplete(buildSources(chunks));
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public List<ChunkInfo> retrieveRelevantChunks(String query, String userId) throws Exception {
        log.debug("Retrieving relevant chunks for query: {}", query);

        try {
            // Generate query vector for KNN search
            float[] queryVector = vectorEmbeddingService.generateEmbedding(query);

            // Perform hybrid search: BM25 + KNN with RRF (Reciprocal Rank Fusion)
            // TODO: Implement proper permission filtering - need to get user's accessible document IDs
            List<Long> accessibleDocIds = null; // null means search all documents

            // BM25 search
            List<ElasticsearchService.SearchHit> bm25Hits = elasticsearchService.searchByBM25(query, accessibleDocIds);
            log.debug("BM25 search returned {} hits", bm25Hits.size());

            // KNN search
            List<ElasticsearchService.SearchHit> knnHits = elasticsearchService.searchByKNN(queryVector, accessibleDocIds);
            log.debug("KNN search returned {} hits", knnHits.size());

            // Merge results using RRF (Reciprocal Rank Fusion)
            List<ChunkInfo> chunks = mergeSearchResults(bm25Hits, knnHits);

            log.info("Retrieved {} chunks for query", chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("Failed to retrieve chunks for query: {}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * Merge BM25 and KNN search results using RRF (Reciprocal Rank Fusion)
     * RRF score = sum(1 / (k + rank)) for each ranking
     */
    private List<ChunkInfo> mergeSearchResults(
            List<ElasticsearchService.SearchHit> bm25Hits,
            List<ElasticsearchService.SearchHit> knnHits) {

        Map<String, ChunkInfo> chunkMap = new HashMap<>();
        int k = 60; // RRF constant

        // Process BM25 results
        for (int i = 0; i < bm25Hits.size(); i++) {
            ElasticsearchService.SearchHit hit = bm25Hits.get(i);
            String key = hit.getId();

            ChunkInfo chunk = chunkMap.computeIfAbsent(key, id -> convertToChunkInfo(hit));
            double rrfScore = 1.0 / (k + i + 1);
            chunk.setScore(chunk.getScore() + rrfScore);
        }

        // Process KNN results
        for (int i = 0; i < knnHits.size(); i++) {
            ElasticsearchService.SearchHit hit = knnHits.get(i);
            String key = hit.getId();

            ChunkInfo chunk = chunkMap.computeIfAbsent(key, id -> convertToChunkInfo(hit));
            double rrfScore = 1.0 / (k + i + 1);
            chunk.setScore(chunk.getScore() + rrfScore);
        }

        // Sort by RRF score and return top results
        return chunkMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(10) // Top 10 chunks
                .collect(Collectors.toList());
    }

    /**
     * Convert Elasticsearch SearchHit to ChunkInfo
     */
    private ChunkInfo convertToChunkInfo(ElasticsearchService.SearchHit hit) {
        return ChunkInfo.builder()
                .id(hit.getId())
                .documentId(hit.getDocumentId())
                .content(hit.getContent())
                .title(hit.getTitle() != null ? hit.getTitle() : "Untitled")
                .fileName(hit.getFileName() != null ? hit.getFileName() : "unknown")
                .score(hit.getScore())
                .highlight(hit.getHighlight())
                .build();
    }

    @Override
    public String buildContext(List<ChunkInfo> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "No relevant context found. Please answer based on your general knowledge.";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkInfo chunk = chunks.get(i);
            context.append(String.format("[Document %d: %s]\n%s\n\n",
                    i + 1, chunk.getTitle(), chunk.getContent()));
        }
        return context.toString();
    }

    @Override
    public String buildRAGPrompt(String question, String context) {
        return String.format("""
                Based on the following context, please answer the question.

                Context:
                %s

                Question: %s

                Please provide a comprehensive answer based on the context above.
                """, context, question);
    }

    private List<Map<String, Object>> buildSources(List<ChunkInfo> chunks) {
        List<Map<String, Object>> sources = new ArrayList<>();
        for (ChunkInfo chunk : chunks) {
            Map<String, Object> source = new HashMap<>();
            source.put("id", chunk.getId());
            source.put("documentId", chunk.getDocumentId());
            source.put("title", chunk.getTitle());
            source.put("fileName", chunk.getFileName());
            source.put("score", chunk.getScore());
            sources.add(source);
        }
        return sources;
    }
}
