# Code Review AI - Monitoring System Guide

## Overview

This document describes the monitoring system implemented for the Code Review AI application using Micrometer, Prometheus, and Grafana.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │  AgentOrchestrator → QwenChatProvider             │ │
│  │       ↓                    ↓                       │ │
│  │  AgentMetrics (Micrometer) ← Record metrics        │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP: /actuator/prometheus
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Prometheus (metrics storage)                │
│  - Scrapes metrics every 15 seconds                     │
│  - Time-series database                                 │
│  - PromQL query language                                │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  Grafana (visualization)                 │
│  - Pre-configured dashboards                            │
│  - Real-time monitoring                                 │
│  - Alert rules (configurable)                           │
└─────────────────────────────────────────────────────────┘
```

## Metrics Collected

### Agent Metrics
- `agent.execution.duration` - Time taken by each agent to complete analysis
- `agent.execution.count` - Total number of agent executions
- `agent.issues.found` - Number of issues found by each agent

### AI API Metrics
- `ai.api.duration` - Time taken for AI API calls (tagged by success/failure)
- `ai.api.tokens` - Total tokens consumed
- `ai.api.errors` - Number of failed API calls

### Review Metrics
- `review.total` - Total code reviews (tagged by language)
- `review.with_issues` - Reviews that found issues (tagged by language)

### Tool Metrics
- `tool.execution.duration` - Time taken for tool execution
- `tool.execution.count` - Total tool executions (tagged by tool name and success)

### Orchestrator Metrics
- `orchestrator.execution.duration` - Total time for orchestrator to complete
- `orchestrator.agents.parallel` - Number of agents running in parallel

## Quick Start

### 1. Start the Monitoring Stack

From the project root directory:

```bash
# Start all services including Prometheus and Grafana
docker-compose up -d
```

### 2. Verify Services

```bash
# Check all services are running
docker-compose ps

# Access Prometheus
# http://localhost:9090

# Access Grafana
# http://localhost:3000 (admin/admin)
```

### 3. Access Metrics Endpoints

```bash
# View Prometheus metrics (after starting the application)
curl http://localhost:8081/actuator/prometheus

# Check application health
curl http://localhost:8081/actuator/health

# List all available metrics
curl http://localhost:8081/actuator/metrics
```

## Grafana Setup

### 1. Add Prometheus Data Source

1. Log in to Grafana (http://localhost:3000)
2. Go to Configuration → Data Sources → Add data source
3. Select Prometheus
4. Set URL to `http://prometheus:9090` (inside Docker network)
5. Click "Save & Test"

### 2. Create Dashboards

You can either:
- Import a pre-configured dashboard JSON
- Create your own dashboard with panels

#### Example Dashboard Configuration

Create a new dashboard and add panels with these PromQL queries:

**Panel 1: Agent Execution Time (Average)**
```promql
rate(agent_execution_duration_sum[5m]) / rate(agent_execution_duration_count[5m])
```

**Panel 2: Agent Execution Count**
```promql
sum by (agent) (increase(agent_execution_count[5m]))
```

**Panel 3: AI API Response Time**
```promql
rate(ai_api_duration_sum{success="true"}[5m]) / rate(ai_api_duration_count{success="true"}[5m])
```

**Panel 4: AI Error Rate**
```promql
sum(rate(ai_api_errors_total[5m])) / sum(rate(agent_execution_count[5m])) * 100
```

**Panel 5: Reviews with Issues**
```promql
sum by (language) (increase(review_with_issues_total[1h]))
```

## Useful PromQL Queries

### Agent Performance
```promql
# Average agent execution time
rate(agent_execution_duration_sum[5m]) / rate(agent_execution_duration_count[5m])

# Agent execution count by agent
sum by (agent) (increase(agent_execution_count[5m]))

# P95 agent execution time
histogram_quantile(0.95, rate(agent_execution_duration_bucket[5m]))
```

### AI API Monitoring
```promql
# AI API error rate
sum(rate(ai_api_errors_total[5m])) / sum(rate(agent_execution_count[5m]))

# AI Token consumption rate
rate(ai_api_tokens_total[1h])

# API calls per second
sum(rate(ai_api_duration_count[1m]))
```

### Review Statistics
```promql
# Reviews finding issues
sum(increase(review_with_issues_total[1h])) / sum(increase(review_total[1h])) * 100

# Reviews by language
sum by (language) (increase(review_total[1h]))
```

### System Health
```promql
# JVM Memory Usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# HTTP Request Rate
sum(rate(http_server_requests_seconds_count[5m]))

# HTTP Error Rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

## Configuration Files

### application.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

### prometheus.yml
- Scrape interval: 15 seconds
- Target: `host.docker.internal:8081` (for local development)
- Adjust targets for production deployments

## Troubleshooting

### Prometheus not scraping metrics

1. Check if the application is running:
   ```bash
   curl http://localhost:8081/actuator/prometheus
   ```

2. Verify Prometheus configuration:
   ```bash
   docker exec kb-prometheus promtool check config /etc/prometheus/prometheus.yml
   ```

3. Check Prometheus targets:
   - Go to http://localhost:9090/targets
   - Verify the codereview-api target is "UP"

### Grafana cannot connect to Prometheus

1. Check if Prometheus is accessible from Grafana container:
   ```bash
   docker exec kb-grafana wget -O- http://prometheus:9090/-/healthy
   ```

2. Verify data source configuration uses `http://prometheus:9090`

### Missing metrics

1. Verify the application has the actuator and micrometer dependencies
2. Check application logs for any Micrometer errors
3. Verify metrics are being generated by accessing `/actuator/prometheus`

## Performance Considerations

- The monitoring system has minimal overhead (<1%)
- Metrics are collected asynchronously
- No blocking operations in the hot path
- Consider adjusting scrape intervals for production:
  - Development: 15 seconds
  - Production: 30-60 seconds

## Next Steps

### Week 2-3: Data Collection
- Run the system with monitoring enabled
- Collect at least 100 code review data points
- Analyze the metrics in Grafana

### Week 4: Analysis & Optimization
Based on the collected data, implement optimizations:

1. **If AI API is the bottleneck**:
   - Optimize prompts to reduce token usage
   - Add result caching
   - Implement request batching

2. **If Agent coordination is slow**:
   - Already using parallel execution
   - Consider optimizing thread pool size
   - Filter agents by language more aggressively

3. **If database is slow**:
   - Add Redis caching
   - Optimize database queries
   - Use connection pooling

## Files Modified/Created

### Modified Files
- `backend/codereview-api/pom.xml` - Added micrometer-registry-prometheus dependency
- `backend/codereview-api/src/main/resources/application.yml` - Enabled prometheus endpoint
- `backend/codereview-domain/src/main/java/com/codereview/ai/domain/agent/AgentOrchestrator.java` - Added metrics recording
- `backend/codereview-infrastructure/src/main/java/com/codereview/ai/infrastructure/ai/chat/QwenChatProvider.java` - Added AI call metrics
- `docker-compose.yml` - Added Prometheus and Grafana services

### New Files
- `backend/codereview-domain/src/main/java/com/codereview/ai/domain/metrics/AgentMetrics.java` - Metrics collection component
- `prometheus.yml` - Prometheus configuration

## References

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
