package com.company.kb.infra.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置类
 * 配置生产者和消费者，支持消息序列化、手动提交offset、错误重试和死信队列
 */
@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted-packages:*}")
    private String trustedPackages;

    /**
     * 配置初始化后执行
     * 用于验证Kafka配置是否正确加载
     */
    @PostConstruct
    public void init() {
        log.info("====================================");
        log.info("✅ KafkaConfig 已初始化");
        log.info("====================================");
        log.info("Bootstrap Servers: {}", bootstrapServers);
        log.info("Consumer Group ID: {}", groupId);
        log.info("Trusted Packages: {}", trustedPackages);
        log.info("====================================");
    }

    /**
     * 生产者配置
     * 配置幂等生产者和消息确认机制，确保消息可靠投递
     *
     * 注意：使用String序列化器发送documentId，避免JsonSerializer处理简单类型时的问题
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 可靠投递配置
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // 全部ISR落盘才确认
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 幂等生产者
        config.put(ProducerConfig.RETRIES_CONFIG, 3); // 自动重试3次
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * 消费者配置
     * 禁用自动提交offset，使用手动确认模式
     *
     * 注意：使用String反序列化器接收documentId，避免JsonDeserializer处理简单类型时的问题
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 禁用自动提交，配合容器的MANUAL ack模式使用
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Kafka监听器容器工厂
     * 配置手动提交offset、并发数、错误重试和死信队列
     *
     * 错误处理策略：
     * 1. 固定间隔重试：每3秒重试一次
     * 2. 最多重试4次（加上首次共5次尝试）
     * 3. 重试失败后发送到死信队列：document-processing-dlt
     */
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {

        log.info("====================================");
        log.info("创建 ConcurrentKafkaListenerContainerFactory");
        log.info("====================================");

        // 配置死信队列发布器：重试失败后自动发送到死信队列
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition("document-processing-dlt", record.partition()));

        log.info("DeadLetterPublishingRecoverer 已配置，目标topic: document-processing-dlt");

        // 配置错误处理器：不重试，直接发送到死信队列（调试用）
        // 生产环境可以启用重试：new FixedBackOff(3000L, 4)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer);

        log.info("DefaultErrorHandler 已配置: 不重试，直接进入DLQ（调试模式）");

        // 配置监听器容器工厂
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler); // 设置错误处理器
        factory.setConcurrency(3);
        // 配置手动确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        log.info("ConcurrentKafkaListenerContainerFactory 创建完成");
        log.info("ACK模式: MANUAL");
        log.info("====================================");

        return factory;
    }
}
