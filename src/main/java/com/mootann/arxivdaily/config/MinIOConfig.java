package com.mootann.arxivdaily.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO对象存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {
    
    /**
     * MinIO服务器地址
     */
    private String endpoint;
    
    /**
     * MinIO访问密钥
     */
    private String accessKey;
    
    /**
     * MinIO密钥
     */
    private String secretKey;
    
    /**
     * MinIO默认存储桶名称
     */
    private String bucketName = "arxiv-daily";
    
    /**
     * 是否在启动时自动创建存储桶
     */
    private Boolean autoCreateBucket = true;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Long connectTimeout = 10000L;
    
    /**
     * 写入超时时间（毫秒）
     */
    private Long writeTimeout = 60000L;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Long readTimeout = 10000L;
}