package com.mootann.arxivdaily.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分类论文数量统计DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCountDTO implements Serializable {
    private String category;
    private Long count;
}
