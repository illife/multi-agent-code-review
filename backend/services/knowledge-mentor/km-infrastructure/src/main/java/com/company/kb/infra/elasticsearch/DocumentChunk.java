package com.company.kb.infra.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档块实体
 * 用于Elasticsearch索引
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 块ID
     */
    private Long chunkId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 块内容
     */
    private String content;

    /**
     * 内容向量（用于KNN搜索）
     */
    private float[] contentVector;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 部门
     */
    private String department;

    /**
     * 标签数组
     */
    private String[] tags;

    /**
     * 上传者ID
     */
    private Long uploadedBy;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 块在文档中的位置
     */
    private Integer position;

    /**
     * 创建时间
     */
    private Long createdAt;
}
