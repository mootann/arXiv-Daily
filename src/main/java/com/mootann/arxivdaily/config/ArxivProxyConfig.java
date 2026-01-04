package com.mootann.arxivdaily.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * arXiv API代理配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "arxiv.proxy")
public class ArxivProxyConfig {
    
    /**
     * 是否启用代理
     */
    private Boolean enabled = false;
    
    /**
     * 代理服务器地址
     */
    private String host;
    
    /**
     * 代理服务器端口
     */
    private Integer port;
}