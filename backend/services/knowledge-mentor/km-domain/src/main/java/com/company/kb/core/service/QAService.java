package com.company.kb.core.service;

import java.util.List;
import java.util.Map;

/**
 * 问答服务接口
 * 提供RAG（检索增强生成）问答功能
 */
public interface QAService {

    /**
     * 处理查询（非流式）
     * @param question 问题
     * @param userId 用户ID
     * @return 答案DTO
     */
    AnswerDTO processQuery(String question, String userId) throws Exception;

    /**
     * 处理查询（流式）
     * @param question 问题
     * @param userId 用户ID
     * @param callback 流式回调
     */
    void processQueryWithStream(
        String question,
        String userId,
        StreamCallback callback
    ) throws Exception;

    /**
     * 检索相关文档块
     * @param query 查询
     * @param userId 用户ID
     * @return 文档块列表
     */
    List<ChunkInfo> retrieveRelevantChunks(
        String query,
        String userId
    ) throws Exception;

    /**
     * 构建上下文
     * @param chunks 文档块
     * @return 上下文字符串
     */
    String buildContext(List<ChunkInfo> chunks);

    /**
     * 构建RAG提示词
     * @param question 问题
     * @param context 上下文
     * @return 提示词
     */
    String buildRAGPrompt(String question, String context);

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        void onToken(String token);
        void onComplete(List<Map<String, Object>> sources);
        void onError(Throwable error);
    }

    /**
     * 答案DTO
     */
    class AnswerDTO {
        private String answer;
        private List<Map<String, Object>> sources;
        private List<Map<String, Object>> citations;

        public static AnswerDTO builder() {
            return new AnswerDTO();
        }

        public AnswerDTO answer(String answer) {
            this.answer = answer;
            return this;
        }

        public AnswerDTO sources(List<Map<String, Object>> sources) {
            this.sources = sources;
            return this;
        }

        public AnswerDTO citations(List<Map<String, Object>> citations) {
            this.citations = citations;
            return this;
        }

        public AnswerDTO build() {
            return this;
        }

        // Getters and Setters
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public List<Map<String, Object>> getSources() { return sources; }
        public void setSources(List<Map<String, Object>> sources) { this.sources = sources; }
        public List<Map<String, Object>> getCitations() { return citations; }
        public void setCitations(List<Map<String, Object>> citations) { this.citations = citations; }
    }
}
