package com.company.kb.core.service;

import java.util.List;

/**
 * 权限服务接口
 * 处理文档权限和用户权限检查
 */
public interface PermissionService {

    /**
     * 获取用户可访问的文档ID列表
     * @param userId 用户ID
     * @return 文档ID列表
     */
    List<Long> getAccessibleDocuments(String userId);

    /**
     * 检查文档权限
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param permissionType 权限类型（READ, WRITE, DELETE, ADMIN）
     * @return 是否有权限
     */
    boolean checkDocumentPermission(String userId, Long documentId, String permissionType);

    /**
     * 授予文档权限
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param permissionType 权限类型
     * @param grantedBy 授予者ID
     */
    void grantDocumentPermission(Long documentId, String userId, String permissionType, String grantedBy);

    /**
     * 撤销文档权限
     * @param documentId 文档ID
     * @param userId 用户ID
     */
    void revokeDocumentPermission(Long documentId, String userId);

    /**
     * 批量授予文档权限
     * @param documentId 文档ID
     * @param userIds 用户ID列表
     * @param permissionType 权限类型
     * @param grantedBy 授予者ID
     */
    void grantDocumentPermissions(Long documentId, List<String> userIds, String permissionType, String grantedBy);

    /**
     * 获取文档的权限列表
     * @param documentId 文档ID
     * @return 权限列表
     */
    List<DocumentPermissionDTO> getDocumentPermissions(Long documentId);

    /**
     * 清除用户缓存
     * @param userId 用户ID
     */
    void evictUserCache(String userId);

    /**
     * 文档权限DTO
     */
    class DocumentPermissionDTO {
        private Long documentId;
        private String userId;
        private String permissionType;
        private String grantedBy;
        private Long grantedAt;

        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getPermissionType() { return permissionType; }
        public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
        public String getGrantedBy() { return grantedBy; }
        public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
        public Long getGrantedAt() { return grantedAt; }
        public void setGrantedAt(Long grantedAt) { this.grantedAt = grantedAt; }
    }
}
