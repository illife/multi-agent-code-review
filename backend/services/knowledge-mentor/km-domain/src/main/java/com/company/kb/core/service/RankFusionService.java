package com.company.kb.core.service;

import java.util.List;

/**
 * RRF（Reciprocal Rank Fusion）融合服务
 * 将BM25和KNN的检索结果融合，提高召回率
 */
public interface RankFusionService {

    /**
     * RRF算法融合
     * @param bm25Results BM25检索结果
     * @param knnResults KNN检索结果
     * @param k RRF参数，默认60
     * @return 融合后的结果列表
     */
    List<ChunkInfo> reciprocalRankFusion(
        List<ChunkInfo> bm25Results,
        List<ChunkInfo> knnResults,
        int k
    );

    /**
     * RRF算法融合（使用默认参数k=60）
     */
    default List<ChunkInfo> reciprocalRankFusion(
        List<ChunkInfo> bm25Results,
        List<ChunkInfo> knnResults) {
        return reciprocalRankFusion(bm25Results, knnResults, 60);
    }
}
