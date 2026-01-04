package com.mootann.arxivdaily.dto.arxiv;

import com.mootann.arxivdaily.dto.CategoryCountDTO;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * arXiv搜索响应结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArxivSearchResponse {
    
    private Integer totalResults;           // 总结果数
    private Integer startIndex;             // 起始索引
    private Integer itemsPerPage;           // 每页数量
    private List<ArxivPaperDTO> papers;     // 论文列表
    private String actualStartDate;         // 实际查询的开始日期(当没有今天的数据时会自动调整)
    private String actualEndDate;           // 实际查询的结束日期
    private List<CategoryCountDTO> categoryCounts; // 分类统计信息
}