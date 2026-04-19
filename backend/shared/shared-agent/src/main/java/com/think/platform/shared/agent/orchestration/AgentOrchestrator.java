package com.think.platform.shared.agent.orchestration;

import com.think.platform.shared.agent.core.Agent;
import com.think.platform.shared.agent.core.AgentRequest;
import com.think.platform.shared.agent.core.AgentResult;
import com.think.platform.shared.agent.core.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent 编排器
 * 负责协调多个 Agent 的执行
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class AgentOrchestrator {

    /**
     * 注册的 Agents
     */
    private final Map<AgentType, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 注册 Agent
     */
    public void registerAgent(Agent agent) {
        agents.put(agent.getAgentType(), agent);
        log.info("Agent registered: {} ({})", agent.getName(), agent.getAgentType());
    }

    /**
     * 注销 Agent
     */
    public void unregisterAgent(AgentType agentType) {
        Agent removed = agents.remove(agentType);
        if (removed != null) {
            log.info("Agent unregistered: {}", agentType);
        }
    }

    /**
     * 获取 Agent
     */
    public Optional<Agent> getAgent(AgentType agentType) {
        return Optional.ofNullable(agents.get(agentType));
    }

    /**
     * 获取所有已注册的 Agents
     */
    public Collection<Agent> getAllAgents() {
        return agents.values();
    }

    /**
     * 执行单个 Agent
     */
    public AgentResult execute(AgentRequest request) {
        Agent agent = agents.get(request.getAgentType());
        if (agent == null) {
            return AgentResult.failure(
                    request.getRequestId(),
                    request.getAgentType(),
                    "Agent not found: " + request.getAgentType()
            );
        }

        if (!agent.isAvailable()) {
            return AgentResult.failure(
                    request.getRequestId(),
                    request.getAgentType(),
                    "Agent not available: " + agent.getName()
            );
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("Executing agent: {} for request: {}", agent.getName(), request.getRequestId());
            AgentResult result = agent.execute(request);
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Agent execution failed: {}", agent.getName(), e);
            return AgentResult.failure(
                    request.getRequestId(),
                    request.getAgentType(),
                    "Agent execution failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 执行多个 Agents (串行)
     */
    public List<AgentResult> executeSequential(AgentRequest baseRequest, List<AgentType> agentTypes) {
        List<AgentResult> results = new ArrayList<>();

        for (AgentType agentType : agentTypes) {
            AgentRequest request = AgentRequest.builder()
                    .requestId(baseRequest.getRequestId() + "-" + agentType.getCode())
                    .agentType(agentType)
                    .inputData(new HashMap<>(baseRequest.getInputData()))
                    .userId(baseRequest.getUserId())
                    .timeout(baseRequest.getTimeout())
                    .build();

            AgentResult result = execute(request);
            results.add(result);

            // 如果失败，可以选择中止后续执行
            if (!result.isSuccess()) {
                log.warn("Agent {} failed, aborting sequential execution", agentType);
                break;
            }

            // 将当前 Agent 的输出合并到下一个 Agent 的输入
            baseRequest.getInputData().putAll(result.getOutputData());
        }

        return results;
    }

    /**
     * 执行多个 Agents (并行)
     */
    public List<AgentResult> executeParallel(AgentRequest baseRequest, List<AgentType> agentTypes) {
        List<CompletableFuture<AgentResult>> futures = new ArrayList<>();

        for (AgentType agentType : agentTypes) {
            AgentRequest request = AgentRequest.builder()
                    .requestId(baseRequest.getRequestId() + "-" + agentType.getCode())
                    .agentType(agentType)
                    .inputData(new HashMap<>(baseRequest.getInputData()))
                    .userId(baseRequest.getUserId())
                    .timeout(baseRequest.getTimeout())
                    .build();

            futures.add(CompletableFuture.supplyAsync(() -> execute(request)));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 执行多个 Agents (流水线)
     */
    public AgentResult executePipeline(AgentRequest baseRequest, List<AgentType> agentTypes) {
        Map<String, Object> pipelineData = new HashMap<>(baseRequest.getInputData());
        AgentResult lastResult = null;

        for (AgentType agentType : agentTypes) {
            AgentRequest request = AgentRequest.builder()
                    .requestId(baseRequest.getRequestId() + "-" + agentType.getCode())
                    .agentType(agentType)
                    .inputData(new HashMap<>(pipelineData))
                    .userId(baseRequest.getUserId())
                    .timeout(baseRequest.getTimeout())
                    .build();

            AgentResult result = execute(request);
            lastResult = result;

            if (!result.isSuccess()) {
                log.warn("Pipeline failed at agent: {}", agentType);
                return result;
            }

            // 将输出合并到流水线数据
            pipelineData.putAll(result.getOutputData());
        }

        // 返回最终结果，包含所有 Agent 的输出
        return AgentResult.success(
                baseRequest.getRequestId(),
                AgentType.ORCHESTRATOR,
                "Pipeline execution completed",
                pipelineData
        );
    }

    /**
     * 使用指定策略执行多个 Agents
     */
    public List<AgentResult> execute(AgentRequest baseRequest, List<AgentType> agentTypes, ExecutionStrategy strategy) {
        return switch (strategy) {
            case SEQUENTIAL -> executeSequential(baseRequest, agentTypes);
            case PARALLEL -> executeParallel(baseRequest, agentTypes);
            case PIPELINE -> Collections.singletonList(executePipeline(baseRequest, agentTypes));
            default -> throw new UnsupportedOperationException("Strategy not supported: " + strategy);
        };
    }

    /**
     * 执行代码审查 (标准流程)
     */
    public List<AgentResult> executeCodeReview(String requestId, String code, String language) {
        AgentRequest request = AgentRequest.builder()
                .requestId(requestId)
                .inputData(Map.of(
                        "code", code,
                        "language", language
                ))
                .build();

        // 并行执行四个检查员 Agent
        List<AgentType> inspectors = Arrays.asList(
                AgentType.CODE_STANDARDS_INSPECTOR,
                AgentType.ARCHITECTURE_GUARDIAN,
                AgentType.SECURITY_AUDITOR,
                AgentType.PERFORMANCE_OPTIMIZER
        );

        log.info("Starting code review with {} inspectors", inspectors.size());
        return executeParallel(request, inspectors);
    }

    /**
     * 执行教学流程
     */
    public AgentResult executeTeaching(String requestId, Long userId, String topic, String userLevel) {
        // 1. 先评估技能
        AgentRequest assessRequest = AgentRequest.builder()
                .requestId(requestId + "-assess")
                .agentType(AgentType.SKILL_ASSESSOR)
                .userId(userId)
                .inputData(Map.of(
                        "topic", topic,
                        "userLevel", userLevel
                ))
                .build();

        AgentResult assessResult = execute(assessRequest);

        // 2. 根据评估结果进行教学
        Map<String, Object> teachingInput = new HashMap<>();
        teachingInput.put("topic", topic);
        teachingInput.put("userLevel", userLevel);
        if (assessResult.isSuccess()) {
            teachingInput.putAll(assessResult.getOutputData());
        }

        AgentRequest teachRequest = AgentRequest.builder()
                .requestId(requestId + "-teach")
                .agentType(AgentType.TEACHING_MENTOR)
                .userId(userId)
                .inputData(teachingInput)
                .build();

        return execute(teachRequest);
    }
}
