package com.company.kb.infra.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic配置类
 * 负责在应用启动时自动创建所需的Kafka topics
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.topic.document-processing:document-processing}")
    private String documentProcessingTopic;

    @Value("${spring.kafka.topic.document-processing-dlt:document-processing-dlt}")
    private String documentProcessingDltTopic;

    /**
     * Kafka Admin配置
     * 用于管理Kafka topics的创建和删除
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 文档处理Topic
     * 用于接收文档上传后的异步处理任务
     *
     * 配置说明：
     * - name: topic名称
     * - partitions: 3个分区，提高并行处理能力
     * - replicas: 1个副本（单节点环境，生产环境建议3个）
     */
    @Bean
    public NewTopic documentProcessingTopic() {
        log.info("创建Kafka Topic: {} (partitions=3, replicas=1)", documentProcessingTopic);
        return TopicBuilder.name(documentProcessingTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 文档处理死信队列Topic
     * 用于接收文档处理失败的重试耗尽消息
     *
     * 配置说明：
     * - name: topic名称
     * - partitions: 3个分区（与主topic保持一致）
     * - replicas: 1个副本（单节点环境，生产环境建议3个）
     */
    @Bean
    public NewTopic documentProcessingDltTopic() {
        log.info("创建Kafka DLT Topic: {} (partitions=3, replicas=1)", documentProcessingDltTopic);
        return TopicBuilder.name(documentProcessingDltTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * AdminClient Bean（可选）
     * 提供更底层的Kafka管理功能，可用于运行时topic管理
     */
    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
