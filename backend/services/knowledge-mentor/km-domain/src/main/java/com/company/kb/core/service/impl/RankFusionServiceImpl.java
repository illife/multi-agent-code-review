package com.company.kb.core.service.impl;

import com.company.kb.core.service.ChunkInfo;
import com.company.kb.core.service.RankFusionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RRF（Reciprocal Rank Fusion）融合服务实现
 *
 * RRF算法原理：
 * score(d) = Σ 1/(k + rank_i(d))
 *
 * 其中：
 * - rank_i(d) 是文档d在第i个结果列表中的排名
 * - k是平滑参数，通常取60
 *
 * RRF优势：
 * 1. 归一化排名，不依赖分数
 * 2. 无需调参，更加robust
 * 3. 学术界和工业界标准方法
 */
@Slf4j
@Service
public class RankFusionServiceImpl implements RankFusionService {

    @Override
    public List<ChunkInfo> reciprocalRankFusion(
            List<ChunkInfo> bm25Results,
            List<ChunkInfo> knnResults,
            int k) {

        log.info("开始RRF融合: bm25Results={}, knnResults={}, k={}",
            bm25Results.size(), knnResults.size(), k);

        // 用于存储每个chunk的RRF分数
        Map<String, RRFScore> rrfScores = new HashMap<>();

        // 处理BM25结果
        for (int i = 0; i < bm25Results.size(); i++) {
            ChunkInfo hit = bm25Results.get(i);
            String key = hit.getId();
            double score = 1.0 / (k + i + 1); // RRF公式

            rrfScores.computeIfAbsent(key, k2 -> new RRFScore(hit))
                    .addScore(score);
        }

        // 处理KNN结果
        for (int i = 0; i < knnResults.size(); i++) {
            ChunkInfo hit = knnResults.get(i);
            String key = hit.getId();
            double score = 1.0 / (k + i + 1); // RRF公式

            rrfScores.computeIfAbsent(key, k2 -> new RRFScore(hit))
                    .addScore(score);
        }

        // 按RRF分数排序并返回结果
        List<ChunkInfo> mergedResults = rrfScores.values().stream()
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .map(RRFScore::getChunk)
                .collect(Collectors.toList());

        log.info("RRF融合完成: mergedResults={}", mergedResults.size());

        return mergedResults;
    }

    /**
     * RRF分数辅助类
     */
    private static class RRFScore {
        private final ChunkInfo chunk;
        private double totalScore = 0.0;

        public RRFScore(ChunkInfo chunk) {
            this.chunk = chunk;
        }

        public void addScore(double score) {
            this.totalScore += score;
        }

        public ChunkInfo getChunk() {
            // 更新chunk的分数为RRF分数
            chunk.setScore(totalScore);
            return chunk;
        }

        public double getTotalScore() {
            return totalScore;
        }
    }
}
