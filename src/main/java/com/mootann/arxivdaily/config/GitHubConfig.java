package com.mootann.arxivdaily.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub API配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubConfig {
    
    /**
     * GitHub API基础URL
     */
    private String apiBaseUrl = "https://api.github.com";
    
    /**
     * GitHub仓库地址
     * 格式: owner/repo，例如: mootann/arXiv-Daily
     */
    private String repository;
    
    /**
     * GitHub访问令牌
     */
    private String accessToken;
    
    /**
     * 是否启用代理
     */
    private Boolean proxyEnabled = false;
    
    /**
     * 代理服务器地址
     */
    private String proxyHost;
    
    /**
     * 代理服务器端口
     */
    private Integer proxyPort;
}
