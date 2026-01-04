package com.mootann.arxivdaily.dto.arxiv;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * arXiv搜索请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArxivSearchRequest {
    
    private String query;            // 搜索关键词
    private Integer maxResults;      // 最大结果数（默认10）
    private String start;            // 起始位置（默认0）
    private String sortBy;           // 排序字段：relevance, lastUpdatedDate submittedDate
    private String sortOrder;        // 排序方向：ascending, descending
    private String source;           // 数据来源：api, db
    private Boolean hasGithub;       // 是否有GitHub URL
}