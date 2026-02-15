package com.mootann.arxivdaily.repository.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户点赞论文实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_likes")
public class UserLike {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("arxiv_id")
    private String arxivId;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
