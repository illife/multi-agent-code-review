package com.company.kb.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应对象
 * 用于包装分页数据，避免Spring Data的PageImpl序列化问题
 *
 * @param <T> 数据类型
 * @author Knowledge Base Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从0开始）
     */
    private int currentPage;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否为第一页
     */
    private boolean first;

    /**
     * 是否为最后一页
     */
    private boolean last;

    /**
     * 从Spring Data的Page对象构建PageResponse
     *
     * @param page Spring Data的Page对象
     * @param <T>  数据类型
     * @return PageResponse对象
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setData(page.getContent());
        response.setTotal(page.getTotalElements());
        response.setCurrentPage(page.getNumber());
        response.setTotalPages(page.getTotalPages());
        response.setPageSize(page.getSize());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }
}
