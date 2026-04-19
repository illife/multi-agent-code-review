package com.company.kb.config;

import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 索引初始化器
 * 应用启动时自动创建索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer implements ApplicationRunner {

    private final ElasticsearchService elasticsearchService;

    @Value("${elasticsearch.index-name:kb_document_chunks}")
    private String indexName;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("开始初始化 Elasticsearch 索引: {}", indexName);
            elasticsearchService.createIndex(indexName);
            log.info("Elasticsearch 索引初始化完成");
        } catch (Exception e) {
            log.error("Elasticsearch 索引初始化失败", e);
            // 不抛出异常，允许应用继续启动
            // 但索引功能将不可用
        }
    }
}
