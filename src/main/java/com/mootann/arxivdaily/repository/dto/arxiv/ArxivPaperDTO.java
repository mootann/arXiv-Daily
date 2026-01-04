package com.mootann.arxivdaily.repository.dto.arxiv;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * arXiv论文信息数据传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArxivPaperDTO {
    private String arxivId;           // arXiv ID，例如：2301.12345
    private String title;             // 论文标题
    private String summary;           // 论文摘要
    private List<String> authors;     // 作者列表
    private LocalDate publishedDate;  // 发布日期
    private LocalDate updatedDate;    // 更新日期
    private String primaryCategory;   // 主要分类，例如：cs.AI
    private List<String> categories;  // 所有分类列表
    private String pdfUrl;            // PDF文件URL
    private String latexUrl;          // LaTeX源码URL
    private String arxivUrl;          // arXiv页面URL
    private String doi;               // DOI（如果有）
    private Integer version;          // 版本号
    private String githubUrl;         // GitHub仓库URL
}