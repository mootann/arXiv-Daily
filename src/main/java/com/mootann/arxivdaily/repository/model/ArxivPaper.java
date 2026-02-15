package com.mootann.arxivdaily.repository.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * arXiv论文实体类
 * 使用 MyBatis Plus 注解映射到 MySQL 的 arxiv_papers 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "arxiv_papers", autoResultMap = true)
public class ArxivPaper {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("arxiv_id")
    private String arxivId;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    /**
     * 作者列表，使用 JSON 格式存储在 MySQL 的 JSON 类型字段中
     */
    @TableField(value = "authors", typeHandler = JacksonTypeHandler.class)
    private List<String> authors;

    @TableField("published_date")
    private LocalDate publishedDate;

    @TableField("updated_date")
    private LocalDate updatedDate;

    @TableField("primary_category")
    private String primaryCategory;

    /**
     * 分类列表，使用 JSON 格式存储在 MySQL 的 JSON 类型字段中
     */
    @TableField(value = "categories", typeHandler = JacksonTypeHandler.class)
    private List<String> categories;

    @TableField("pdf_url")
    private String pdfUrl;

    @TableField("latex_url")
    private String latexUrl;

    @TableField("arxiv_url")
    private String arxivUrl;

    @TableField("doi")
    private String doi;

    @TableField("version")
    private Integer version;

    /**
     * arXiv元数据中的评论信息（注意：这不是用户评论）
     */
    @TableField("comment")
    private String comment;

    @TableField("journal_ref")
    private String journalRef;

    @TableField("github_url")
    private String githubUrl;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField(exist = false)
    private Long likeCount;

    @TableField(exist = false)
    private Long collectCount;

    @TableField(exist = false)
    private Long commentCount;

    @TableField(exist = false)
    private Long viewCount;

    @TableField(exist = false)
    private Boolean isLiked;

    @TableField(exist = false)
    private Boolean isCollected;
}
