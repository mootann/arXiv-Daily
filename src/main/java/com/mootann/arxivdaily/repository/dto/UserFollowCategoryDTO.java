package com.mootann.arxivdaily.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowCategoryDTO {
    private String category;
    private Long todayPaperCount;
    private LocalDate latestPaperDate;
    private Long paperCount;
}
