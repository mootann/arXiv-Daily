package com.mootann.arxivdaily.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页缓存DTO
 * 用于Redis缓存，避免直接序列化Page对象导致的问题
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageCacheDTO<T> {
    /**
     * 内容列表
     */
    private List<T> content;
    
    /**
     * 页码（从0开始）
     */
    private int page;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 总元素数
     */
    private long totalElements;
    
    /**
     * 总页数
     */
    private int totalPages;
}
