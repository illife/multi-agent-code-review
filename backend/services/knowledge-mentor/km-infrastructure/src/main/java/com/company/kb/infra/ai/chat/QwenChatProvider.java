package com.company.kb.infra.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 千问对话生成提供商
 * 实现阿里云通义千问的对话生成功能
 */
@Slf4j
@Component
@org.springframework.context.annotation.Primary
public class QwenChatProvider implements ChatProvider {

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.api-url}")
    private String apiUrl;

    @Value("${qwen.chat-model}")
    private String chatModel;

    @Value("${qwen.max-tokens:2000}")
    private int maxTokens;

    @Value("${qwen.temperature:0.7}")
    private double temperature;

    private final WebClient.Builder webClientBuilder = WebClient.builder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateAnswer(String question, String context) throws Exception {
        log.debug("千问生成答案: questionLength={}, contextLength={}",
            question.length(), context.length());

        try {
            Map<String, Object> requestBody = buildChatRequest(question, context, false);

            String response = webClientBuilder.build()
                .post()
                .uri(apiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            String answer = root.path("choices").get(0).path("message").path("content").asText();

            log.debug("千问答案生成成功: answerLength={}", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("千问答案生成失败", e);
            throw new Exception("答案生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamAnswer(String question, String context, StreamCallback callback) throws Exception {
        log.info("千问流式生成答案: questionLength={}, contextLength={}",
            question.length(), context.length());

        Map<String, Object> requestBody = buildChatRequest(question, context, true);

        // 使用独立的线程处理流式响应
        CompletableFuture.runAsync(() -> {
            try {
                webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnComplete(() -> {
                        log.info("千问流式生成完成，调用onComplete");
                        callback.onComplete();
                    })
                    .doOnError(error -> {
                        log.error("千问流式请求失败", error);
                        callback.onError(error);
                    })
                    .subscribe(
                        response -> {
                            try {
                                log.info("收到流式响应块: [{}]", response);

                                if ("[DONE]".equals(response.trim())) {
                                    return;
                                }

                                JsonNode root = objectMapper.readTree(response);
                                JsonNode choices = root.path("choices");

                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    JsonNode contentNode = delta.path("content");

                                    if (contentNode.isTextual()) {
                                        String content = contentNode.asText();
                                        if (!content.isEmpty()) {
                                            callback.onToken(content);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("处理流式响应块失败", e);
                                callback.onError(e);
                            }
                        }
                    );
            } catch (Exception e) {
                log.error("启动流式请求失败", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    /**
     * 构建聊天请求（OpenAI兼容格式）
     */
    private Map<String, Object> buildChatRequest(String question, String context, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", chatModel);

        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词
        messages.add(new HashMap<String, String>() {{
            put("role", "system");
            put("content", buildSystemPrompt());
        }});

        // 用户问题（包含上下文）
        String userPrompt = buildUserPrompt(question, context);
        messages.add(new HashMap<String, String>() {{
            put("role", "user");
            put("content", userPrompt);
        }});

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", stream);

        return requestBody;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return "你是一个专业的企业知识库助手。请根据提供的上下文信息回答用户的问题。" +
            "如果上下文中没有相关信息，请明确告知用户。" +
            "回答时请保持专业、准确、简洁的风格。" +
            "必要时可以在回答中引用来源。";
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(String question, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("上下文信息：\n");
        prompt.append(context);
        prompt.append("\n\n");
        prompt.append("用户问题：\n");
        prompt.append(question);
        prompt.append("\n\n");
        prompt.append("请根据上下文信息回答用户问题。");
        return prompt.toString();
    }
}
