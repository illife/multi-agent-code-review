package com.codereview.ai.domain.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka生产者服务
 * 发送项目分析事件到Kafka
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // Kafka主题名称
    public static final String PROJECT_ANALYSIS_TOPIC = "project-analysis";
    public static final String PROJECT_UPLOAD_TOPIC = "project-upload";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送项目分析事件
     *
     * @param projectId 项目ID
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, String>> sendProjectAnalysisEvent(Long projectId) {
        String message = projectId.toString();

        ProducerRecord<String, String> record = new ProducerRecord<>(
                PROJECT_ANALYSIS_TOPIC,
                message
        );

        log.info("Sending project analysis event: projectId={}", projectId);
        return kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Project analysis event sent successfully: projectId={}, partition={}, offset={}",
                                projectId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send project analysis event: projectId={}", projectId, ex);
                    }
                });
    }

    /**
     * 发送项目上传事件
     *
     * @param projectId 项目ID
     * @param projectName 项目名称
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, String>> sendProjectUploadEvent(Long projectId, String projectName) {
        String message = String.format("{\"projectId\":%d,\"projectName\":\"%s\"}", projectId, projectName);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                PROJECT_UPLOAD_TOPIC,
                message
        );

        log.info("Sending project upload event: projectId={}, projectName={}", projectId, projectName);
        return kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Project upload event sent successfully: projectId={}, partition={}, offset={}",
                                projectId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send project upload event: projectId={}", projectId, ex);
                    }
                });
    }

    /**
     * 同步发送消息（等待确认）
     *
     * @param topic Kafka主题
     * @param message 消息内容
     */
    public void sendSync(String topic, String message) {
        try {
            kafkaTemplate.send(topic, message).get();
            log.debug("Message sent synchronously: topic={}, message={}", topic, message);
        } catch (Exception e) {
            log.error("Failed to send message synchronously: topic={}, message={}", topic, message, e);
            throw new RuntimeException("Failed to send Kafka message", e);
        }
    }
}
