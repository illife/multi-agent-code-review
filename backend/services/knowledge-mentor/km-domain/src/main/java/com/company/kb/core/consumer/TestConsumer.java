package com.company.kb.core.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 测试消费者
 * 用于验证Kafka连接和消息传递是否正常
 */
@Slf4j
// @Component  // 临时禁用，让DocumentProcessingConsumer处理消息
public class TestConsumer {

    @KafkaListener(
        topics = "document-processing",
        groupId = "test-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void testConsume(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        log.info("====================================");
        log.info("🧪 TEST CONSUMER 被调用");
        log.info("Topic: {}", record.topic());
        log.info("Partition: {}", record.partition());
        log.info("Offset: {}", record.offset());
        log.info("Key: {}", record.key());
        log.info("Value: {}", record.value());
        log.info("====================================");

        // 确认消息
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }
}
