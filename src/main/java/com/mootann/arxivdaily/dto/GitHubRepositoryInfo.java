package com.mootann.arxivdaily.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub仓库信息DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryInfo {
    
    /**
     * 仓库完整名称 (owner/repo)
     */
    @JsonProperty("full_name")
    private String fullName;
    
    /**
     * 仓库描述
     */
    private String description;
    
    /**
     * Stars数量
     */
    @JsonProperty("stargazers_count")
    private Integer starsCount;
    
    /**
     * Fork数量
     */
    @JsonProperty("forks_count")
    private Integer forksCount;
    
    /**
     * 观察者数量
     */
    @JsonProperty("subscribers_count")
    private Integer subscribersCount;
    
    /**
     * 开放Issue数量
     */
    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;
    
    /**
     * 仓库URL
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private String createdTime;
    
    /**
     * 最后更新时间
     */
    @JsonProperty("updated_at")
    private String updatedTime;
}
