package com.company.kb.controller;

import com.think.platform.shared.common.result.Result;
import com.company.kb.core.service.QAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Question and Answer Controller
 * Provides intelligent Q&A API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QuestionController {

    private final QAService qaService;

    /**
     * Submit a question (non-streaming)
     * @param request Question request
     * @return Answer
     */
    @PostMapping("/ask")
    public Result<QAService.AnswerDTO> askQuestion(
            @RequestBody QuestionQueryRequest request,
            Authentication authentication) {

        try {
            String userId = authentication.getName();
            log.info("Received question request: userId={}, question={}", userId, request.getQuestion());

            QAService.AnswerDTO answer = qaService.processQuery(request.getQuestion(), userId);

            return Result.success(answer);

        } catch (Exception e) {
            log.error("Question processing failed", e);
            return Result.failed(500, "Question processing failed: " + e.getMessage());
        }
    }

    /**
     * Retrieve relevant document chunks
     * @param request Query request
     * @return Relevant chunks
     */
    @PostMapping("/retrieve")
    public Result<List<com.company.kb.core.service.ChunkInfo>> retrieveChunks(
            @RequestBody QuestionQueryRequest request,
            Authentication authentication) {

        try {
            String userId = authentication.getName();
            log.info("Retrieving chunks: userId={}, question={}", userId, request.getQuestion());

            List<com.company.kb.core.service.ChunkInfo> chunks = qaService.retrieveRelevantChunks(request.getQuestion(), userId);

            return Result.success(chunks);

        } catch (Exception e) {
            log.error("Chunk retrieval failed", e);
            return Result.failed(500, "Chunk retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Question query request DTO
     */
    public static class QuestionQueryRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }
}
