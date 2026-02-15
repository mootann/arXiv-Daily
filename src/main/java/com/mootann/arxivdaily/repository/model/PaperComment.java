package com.mootann.arxivdaily.repository.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 论文评论实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("paper_comments")
public class PaperComment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("arxiv_id")
    private String arxivId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("content")
    private String content;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
