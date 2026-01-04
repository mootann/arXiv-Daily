package com.mootann.arxivdaily.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka消息队列配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    
    /**
     * Kafka服务器地址
     * 格式: host1:port1,host2:port2,...
     */
    private String bootstrapServers = "localhost:9092";
    
    /**
     * 生产者配置
     */
    private Producer producer = new Producer();
    
    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 是否启用Kafka
     */
    private Boolean enabled = true;
    
    /**
     * 生产者配置
     */
    @Data
    public static class Producer {
        
        /**
         * 消息确认机制
         * all: 等待所有副本确认
         * 1: 等待leader副本确认
         * 0: 不需要确认
         */
        private String acks = "all";
        
        /**
         * 重试次数
         */
        private Integer retries = 3;
        
        /**
         * 批量发送大小（字节）
         */
        private Long batchSize = 16384L;
        
        /**
         * 缓冲区大小（字节）
         */
        private Long bufferMemory = 33554432L;
        
        /**
         * 默认主题
         */
        private String defaultTopic = "arxiv-daily";
        
        /**
         * 消息压缩类型
         * none, gzip, snappy, lz4, zstd
         */
        private String compressionType = "none";
        
        /**
         * 消息发送超时时间（毫秒）
         */
        private Long requestTimeoutMs = 30000L;
        
        /**
         * 等待acks的超时时间（毫秒）
         */
        private Long deliveryTimeoutMs = 120000L;
        
        /**
         * 是否启用幂等性
         */
        private Boolean enableIdempotence = true;
    }
    
    /**
     * 消费者配置
     */
    @Data
    public static class Consumer {
        
        /**
         * 消费者组ID
         */
        private String groupId = "arxiv-daily-group";
        
        /**
         * 自动提交偏移量
         */
        private Boolean enableAutoCommit = true;
        
        /**
         * 自动提交间隔时间（毫秒）
         */
        private Long autoCommitIntervalMs = 5000L;
        
        /**
         * 如果没有初始偏移量，策略为最早
         * latest, earliest, none
         */
        private String autoOffsetReset = "latest";
        
        /**
         * 一次拉取最大记录数
         */
        private Integer maxPollRecords = 500;
        
        /**
         * 拉取间隔时间（毫秒）
         */
        private Long fetchMaxWaitMs = 500L;
        
        /**
         * 会话超时时间（毫秒）
         */
        private Long sessionTimeoutMs = 30000L;
        
        /**
         * 心跳间隔时间（毫秒）
         */
        private Long heartbeatIntervalMs = 10000L;
        
        /**
         * 默认主题列表
         */
        private String[] topics = new String[]{"arxiv-daily"};
        
        /**
         * 消费者并发数
         */
        private Integer concurrency = 1;
        
        /**
         * 是否启用批量消费
         */
        private Boolean batchListener = false;
    }
}
