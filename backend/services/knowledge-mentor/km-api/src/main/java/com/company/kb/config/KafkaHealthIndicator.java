package com.company.kb.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka健康检查指示器
 * 通过Spring Boot Actuator暴露Kafka集群的健康状态
 *
 * 功能说明：
 * - 检查Kafka集群是否可访问
 * - 检查Kafka集群节点数量
 * - 检查Kafka集群控制器状态
 *
 * 访问方式：
 * - HTTP: GET /api/actuator/health/kafka
 * - 返回示例：
 *   {
 *     "status": "UP",
 *     "details": {
 *       "clusterId": "kafka-cluster-id",
 *       "nodes": 1,
 *       "controller": "localhost:9093"
 *     }
 *   }
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AdminClient.class)
@ConditionalOnProperty(
        prefix = "management.health.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaHealthIndicator implements HealthIndicator {

    private final AdminClient adminClient;

    @Override
    public Health health() {
        try {
            // 描述Kafka集群信息
            DescribeClusterResult result = adminClient.describeCluster(
                    new DescribeClusterOptions().timeoutMs(5000));

            // 获取集群ID
            String clusterId = result.clusterId().get(5, TimeUnit.SECONDS);

            // 获取集群节点数量
            int nodeCount = result.nodes().get(5, TimeUnit.SECONDS).size();

            // 获取控制器节点
            String controller = result.controller().get(5, TimeUnit.SECONDS).host();

            // 构建健康检查详情
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .withDetail("controller", controller)
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Kafka健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }
}
