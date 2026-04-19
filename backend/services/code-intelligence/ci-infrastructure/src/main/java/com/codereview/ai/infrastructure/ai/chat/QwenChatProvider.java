package com.codereview.ai.infrastructure.ai.chat;

import com.codereview.ai.domain.metrics.AgentMetrics;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Qwen AI Chat Provider Implementation
 *
 * Integrates with Alibaba Cloud Qwen (通义千问) API for chat completion
 * Includes retry mechanism and rate limiting to handle 429 errors
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class QwenChatProvider implements ChatProvider, com.codereview.ai.domain.ai.ChatClient {

    private final AgentMetrics agentMetrics;

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

    @Value("${qwen.max-concurrent-requests:3}")
    private int maxConcurrentRequests;

    @Value("${qwen.retry-max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${qwen.retry-initial-delay:1000}")
    private long retryInitialDelayMs;

    private final WebClient.Builder webClientBuilder = WebClient.builder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Rate limiter to prevent 429 errors
    private Semaphore rateLimiter;

    public QwenChatProvider(AgentMetrics agentMetrics) {
        this.agentMetrics = agentMetrics;
    }

    @PostConstruct
    public void init() {
        this.rateLimiter = new Semaphore(maxConcurrentRequests);
        log.info("===================================================");
        log.info("QwenChatProvider Configuration:");
        log.info("  API URL: {}", apiUrl);
        log.info("  Chat Model: {}", chatModel);
        log.info("  Max Tokens: {}", maxTokens);
        log.info("  Temperature: {}", temperature);
        log.info("  Max Concurrent Requests: {}", maxConcurrentRequests);
        log.info("  Retry Max Attempts: {}", retryMaxAttempts);
        log.info("  Retry Initial Delay: {}ms", retryInitialDelayMs);
        log.info("  API Key: {}...", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "***" : "NULL");
        log.info("===================================================");
    }

    @Override
    public String generateAnswer(String question, String context) {
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            log.info("Qwen generating answer: questionLength={}, contextLength={}",
                    question.length(), context != null ? context.length() : 0);

            // Acquire permit for rate limiting
            acquirePermit();

            ChatRequest request = buildChatRequest(question, context, false);

            String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryInitialDelayMs))
                            .filter(this::isRetryableError)
                            .doBeforeRetry(signal -> log.warn("Retrying Qwen API request, attempt: {}/{}, error: {}",
                                    signal.totalRetries() + 1, retryMaxAttempts, signal.failure().getMessage())))
                    .block(Duration.ofSeconds(60));

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String answer = choices.get(0).path("message").path("content").asText();
                    log.info("Qwen answer generated successfully: answerLength={}", answer.length());
                    success = true;
                    return answer;
                }
            }

            log.warn("Empty response from Qwen API");
            return "No response generated";

        } catch (Exception e) {
            log.error("Failed to generate answer from Qwen", e);
            return "Error: " + e.getMessage();
        } finally {
            rateLimiter.release();

            // Record AI call metrics
            long duration = System.currentTimeMillis() - startTime;
            agentMetrics.recordAICall(duration, 0, success);
        }
    }

    @Override
    public void streamAnswer(String question, String context, StreamCallback callback) {
        executorService.submit(() -> {
            long startTime = System.currentTimeMillis();
            AtomicBoolean success = new AtomicBoolean(false);

            try {
                log.info("Qwen streaming answer: questionLength={}, contextLength={}",
                        question.length(), context != null ? context.length() : 0);

                // Acquire permit for rate limiting
                acquirePermit();

                ChatRequest request = buildChatRequest(question, context, true);
                String fullUrl = apiUrl + "/chat/completions";

                log.info("Sending request to: {}", fullUrl);
                log.info("Request model: {}", request.getModel());

                webClientBuilder.build()
                        .post()
                        .uri(fullUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .retryWhen(Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryInitialDelayMs))
                                .filter(this::isRetryableError)
                                .doBeforeRetry(signal -> log.warn("Retrying Qwen streaming request, attempt: {}/{}",
                                        signal.totalRetries() + 1, retryMaxAttempts)))
                        .doOnComplete(() -> {
                            log.info("Qwen streaming completed");
                            success.set(true);
                            callback.onComplete();
                        })
                        .doOnError(error -> {
                            log.error("Qwen streaming failed", error);
                            callback.onError(error);
                        })
                        .doFinally(signal -> {
                            rateLimiter.release();

                            // Record AI call metrics
                            long duration = System.currentTimeMillis() - startTime;
                            agentMetrics.recordAICall(duration, 0, success.get());
                        })
                        .subscribe(response -> {
                            try {
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
                                log.error("Failed to parse streaming response", e);
                            }
                        });

            } catch (Exception e) {
                log.error("Failed to start streaming", e);
                rateLimiter.release();

                // Record AI call metrics for failed request
                long duration = System.currentTimeMillis() - startTime;
                agentMetrics.recordAICall(duration, 0, false);

                callback.onError(e);
            }
        });
    }

    /**
     * Acquire permit with timeout for rate limiting
     */
    private void acquirePermit() {
        try {
            boolean acquired = rateLimiter.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire rate limiter permit, waiting...");
                rateLimiter.acquire(); // Block until permit is available
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for rate limiter permit", e);
        }
    }

    /**
     * Check if the error is retryable (429, 500, 503, etc.)
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webClientException) {
            int status = webClientException.getStatusCode().value();
            boolean isRetryable = status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
            if (isRetryable) {
                log.warn("Retryable error detected: {} - {}", status, webClientException.getStatusText());
            }
            return isRetryable;
        }
        return false;
    }

    // ========================================================================
    // Function Calling Support
    // ========================================================================

    private static final int MAX_TOOL_CALL_ITERATIONS = 10;

    @Override
    public String chatWithTools(String userMessage,
                                 List<ChatProvider.ChatMessage> history,
                                 List<ChatProvider.ToolDefinition> tools,
                                 ChatProvider.ToolCallHandler handler) {
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            log.info("Qwen chat with tools: tools={}, historyLength={}",
                    tools.size(), history != null ? history.size() : 0);

            acquirePermit();

            // Build initial messages list
            List<ChatProvider.ChatMessage> messages = new ArrayList<>();

            // Add history if provided
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }

            // Add current user message if provided
            if (userMessage != null && !userMessage.isEmpty()) {
                messages.add(new ChatProvider.ChatMessage("user", userMessage));
            }

            // ReAct loop: continue calling tools until AI gives final answer
            for (int iteration = 0; iteration < MAX_TOOL_CALL_ITERATIONS; iteration++) {
                log.debug("Tool calling iteration {}/{}", iteration + 1, MAX_TOOL_CALL_ITERATIONS);

                ChatRequest request = buildChatRequestWithTools(messages, tools);

                String response = webClientBuilder.build()
                        .post()
                        .uri(apiUrl + "/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(String.class)
                        .retryWhen(Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryInitialDelayMs))
                                .filter(this::isRetryableError)
                                .doBeforeRetry(signal -> log.warn("Retrying Qwen API request, attempt: {}/{}",
                                        signal.totalRetries() + 1, retryMaxAttempts)))
                        .block(Duration.ofSeconds(60));

                // Parse response and check for tool calls
                ToolCallResult result = parseResponseWithTools(response);

                if (result.hasToolCalls()) {
                    // Execute tools and add results to messages
                    for (ToolCallInfo toolCall : result.getToolCalls()) {
                        log.info("Executing tool: {} with args: {}", toolCall.name, toolCall.arguments);

                        String toolResult = handler.onToolCall(new ChatProvider.ToolCall(toolCall.name, toolCall.arguments));

                        // Add assistant message with tool call
                        messages.add(new ChatProvider.ChatMessage("assistant", result.getContent()));

                        // Add tool result message
                        messages.add(new ChatProvider.ChatMessage("tool", toolResult));

                        log.debug("Tool executed: {}, result length: {}", toolCall.name, toolResult.length());
                    }
                    // Continue loop to let AI process tool results
                } else {
                    // No tool calls, this is the final answer
                    success = true;
                    rateLimiter.release();

                    // Record AI call metrics
                    long duration = System.currentTimeMillis() - startTime;
                    agentMetrics.recordAICall(duration, 0, true);

                    return result.getContent();
                }
            }

            log.warn("Reached maximum tool call iterations without final answer");
            rateLimiter.release();

            // Record AI call metrics for timeout
            long duration = System.currentTimeMillis() - startTime;
            agentMetrics.recordAICall(duration, 0, false);

            return "Error: Maximum tool call iterations reached";

        } catch (Exception e) {
            log.error("Failed to chat with tools from Qwen", e);
            rateLimiter.release();

            // Record AI call metrics for failed request
            long duration = System.currentTimeMillis() - startTime;
            agentMetrics.recordAICall(duration, 0, false);

            return "Error: " + e.getMessage();
        }
    }

    @Override
    public void streamChatWithTools(String userMessage,
                                     List<ChatProvider.ChatMessage> history,
                                     List<ChatProvider.ToolDefinition> tools,
                                     ChatProvider.ToolCallHandler handler,
                                     StreamCallback callback) {
        executorService.submit(() -> {
            try {
                log.info("Qwen streaming chat with tools");

                acquirePermit();

                List<ChatProvider.ChatMessage> messages = new ArrayList<>();

                if (history != null && !history.isEmpty()) {
                    messages.addAll(history);
                }

                if (userMessage != null && !userMessage.isEmpty()) {
                    messages.add(new ChatProvider.ChatMessage("user", userMessage));
                }

                ChatRequest request = buildChatRequestWithTools(messages, tools);

                webClientBuilder.build()
                        .post()
                        .uri(apiUrl + "/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .retryWhen(Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryInitialDelayMs))
                                .filter(this::isRetryableError))
                        .doOnComplete(() -> {
                            log.info("Qwen streaming with tools completed");
                            callback.onComplete();
                        })
                        .doOnError(error -> {
                            log.error("Qwen streaming with tools failed", error);
                            callback.onError(error);
                        })
                        .doFinally(signal -> rateLimiter.release())
                        .subscribe(response -> {
                            try {
                                if ("[DONE]".equals(response.trim())) {
                                    return;
                                }

                                // Handle streaming response with potential tool calls
                                // For simplicity, this implementation focuses on non-streaming tool calls
                                callback.onToken(response);

                            } catch (Exception e) {
                                log.error("Failed to parse streaming response", e);
                            }
                        });

            } catch (Exception e) {
                log.error("Failed to start streaming with tools", e);
                rateLimiter.release();
                callback.onError(e);
            }
        });
    }

    /**
     * Build chat request with tools (Function Calling)
     */
    private ChatRequest buildChatRequestWithTools(
            List<ChatProvider.ChatMessage> messages,
            List<ChatProvider.ToolDefinition> tools) {

        ChatRequest request = new ChatRequest();
        request.setModel(chatModel);
        request.setStream(false);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);
        request.setMessages(convertToRequestMessages(messages));

        // Only set tools if not empty - Qwen API may not accept empty tools array
        List<Map<String, Object>> qwenTools = convertToQwenTools(tools);
        if (!qwenTools.isEmpty()) {
            request.setTools(qwenTools);
        }

        return request;
    }

    /**
     * Convert ChatMessage records to request ChatMessage objects
     */
    private List<ChatProvider.ChatMessage> convertToRequestMessages(List<ChatProvider.ChatMessage> messages) {
        return messages; // Can use records directly since they match the JSON structure
    }

    /**
     * Convert tool definitions to Qwen API format
     */
    private List<Map<String, Object>> convertToQwenTools(List<ChatProvider.ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> qwenTools = new ArrayList<>();
        for (ChatProvider.ToolDefinition tool : tools) {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");

            Map<String, Object> functionDef = new HashMap<>();
            functionDef.put("name", tool.name());
            functionDef.put("description", tool.description());

            // Parse parameters JSON
            try {
                JsonNode paramsNode = objectMapper.readTree(tool.parameters());
                functionDef.put("parameters", paramsNode);
            } catch (Exception e) {
                log.warn("Failed to parse parameters for tool: {}, using empty schema", tool.name(), e);
                functionDef.put("parameters", Map.of(
                        "type", "object",
                        "properties", new HashMap<>()
                ));
            }

            toolDef.put("function", functionDef);
            qwenTools.add(toolDef);
        }

        return qwenTools;
    }

    /**
     * Parse response that may contain tool calls
     * Returns ToolCallResult with tool calls info and content
     */
    private ToolCallResult parseResponseWithTools(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");

                // Check for tool_calls
                if (message.has("tool_calls") && message.path("tool_calls").size() > 0) {
                    JsonNode toolCalls = message.path("tool_calls");
                    List<ToolCallInfo> toolCallList = new ArrayList<>();

                    // Build tool call info and reconstruct assistant message
                    StringBuilder assistantMessage = new StringBuilder();

                    // Add content if present
                    if (message.has("content") && !message.path("content").isNull()) {
                        assistantMessage.append(message.path("content").asText());
                    }

                    for (JsonNode toolCall : toolCalls) {
                        String toolName = toolCall.path("function").path("name").asText();
                        String argsStr = toolCall.path("function").path("arguments").asText();
                        String toolId = toolCall.path("id").asText();

                        Map<String, Object> arguments;
                        try {
                            arguments = objectMapper.readValue(argsStr,
                                    new TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {
                            log.error("Failed to parse tool arguments", e);
                            arguments = Map.of();
                        }

                        toolCallList.add(new ToolCallInfo(toolName, arguments, toolId));

                        // Append to assistant message for context
                        assistantMessage.append(String.format("[调用工具: %s]", toolName));
                    }

                    return new ToolCallResult(true, toolCallList, assistantMessage.toString());
                }

                // No tool calls, return content
                String content = message.has("content") ? message.path("content").asText() : "";
                return new ToolCallResult(false, List.of(), content);
            }

            log.warn("Empty or invalid response from Qwen API");
            return new ToolCallResult(false, List.of(), "No response generated");

        } catch (Exception e) {
            log.error("Failed to parse response with tools", e);
            return new ToolCallResult(false, List.of(), "Error: " + e.getMessage());
        }
    }

    /**
     * Helper class for tool call results
     */
    private static class ToolCallResult {
        private final boolean hasToolCalls;
        private final List<ToolCallInfo> toolCalls;
        private final String content;

        public ToolCallResult(boolean hasToolCalls, List<ToolCallInfo> toolCalls, String content) {
            this.hasToolCalls = hasToolCalls;
            this.toolCalls = toolCalls;
            this.content = content;
        }

        public boolean hasToolCalls() { return hasToolCalls; }
        public List<ToolCallInfo> getToolCalls() { return toolCalls; }
        public String getContent() { return content; }
    }

    /**
     * Helper class for tool call information
     */
    private static class ToolCallInfo {
        private final String name;
        private final Map<String, Object> arguments;
        private final String id;

        public ToolCallInfo(String name, Map<String, Object> arguments, String id) {
            this.name = name;
            this.arguments = arguments;
            this.id = id;
        }

        public String name() { return name; }
        public Map<String, Object> arguments() { return arguments; }
        public String id() { return id; }
    }

    private ChatRequest buildChatRequest(String question, String context, boolean stream) {
        ChatRequest request = new ChatRequest();
        request.setModel(chatModel);
        request.setStream(stream);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);

        List<ChatProvider.ChatMessage> messages = new ArrayList<>();

        // Add system context if provided
        if (context != null && !context.isEmpty()) {
            messages.add(new ChatProvider.ChatMessage("system", context));
        }

        // Add user question
        messages.add(new ChatProvider.ChatMessage("user", question));

        request.setMessages(convertToRequestMessages(messages));

        return request;
    }

    // ========================================================================
    // Request/Response DTOs
    // ========================================================================
    // ChatClient Interface Implementation (Domain Layer)
    // ========================================================================

    @Override
    public String chatWithTools(String userMessage,
                                 List<com.codereview.ai.domain.ai.ChatClient.ChatMessage> history,
                                 List<com.codereview.ai.domain.ai.ChatClient.ToolDefinition> tools,
                                 com.codereview.ai.domain.ai.ChatClient.ToolCallHandler handler) {
        // Convert domain layer types to infrastructure layer types
        List<ChatProvider.ChatMessage> infraHistory = new ArrayList<>();
        for (com.codereview.ai.domain.ai.ChatClient.ChatMessage msg : history) {
            infraHistory.add(new ChatProvider.ChatMessage(msg.role(), msg.content()));
        }

        List<ChatProvider.ToolDefinition> infraTools = new ArrayList<>();
        for (com.codereview.ai.domain.ai.ChatClient.ToolDefinition tool : tools) {
            infraTools.add(new ChatProvider.ToolDefinition(tool.name(), tool.description(), tool.parameters()));
        }

        ChatProvider.ToolCallHandler infraHandler = new ChatProvider.ToolCallHandler() {
            @Override
            public String onToolCall(ChatProvider.ToolCall toolCall) {
                return handler.onToolCall(new com.codereview.ai.domain.ai.ChatClient.ToolCall(toolCall.name(), toolCall.arguments()));
            }
        };

        return chatWithTools(userMessage, infraHistory, infraTools, infraHandler);
    }

    // ========================================================================

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ChatRequest {
        private String model;
        private List<ChatProvider.ChatMessage> messages;
        private boolean stream;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private List<Map<String, Object>> tools;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public List<ChatProvider.ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatProvider.ChatMessage> messages) { this.messages = messages; }

        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

        public List<Map<String, Object>> getTools() { return tools; }
        public void setTools(List<Map<String, Object>> tools) { this.tools = tools; }
    }
}
