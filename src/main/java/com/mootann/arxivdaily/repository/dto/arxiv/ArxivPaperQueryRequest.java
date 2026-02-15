package com.mootann.arxivdaily.repository.dto.arxiv;

import lombok.Data;

import java.util.List;

/**
 * 论文分页查询请求DTO
 * 支持根据分类、关键词、日期范围等条件进行分页查询
 */
@Data
public class ArxivPaperQueryRequest {

    /**
     * 论文分类列表（可选），支持多分类查询
     */
    private List<String> category;

    /**
     * 关键词搜索（可选），支持标题和摘要模糊匹配
     */
    private String keyword;

    /**
     * 开始日期（可选），格式：yyyy-MM-dd
     */
    private String startDate;

    /**
     * 结束日期（可选），格式：yyyy-MM-dd
     */
    private String endDate;

    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页数量，默认10
     */
    private Integer size = 10;

    /**
     * 是否筛选有GitHub链接的论文（可选）
     * true: 只返回有GitHub链接的论文
     * false: 只返回没有GitHub链接的论文
     * null: 不筛选
     */
    private Boolean hasGithub;
}
