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

    private String bootstrapServers;

    private Producer producer = new Producer();

    private Consumer consumer = new Consumer();

    private Boolean enabled = true;
    
    /**
     * 生产者配置
     */
    @Data
    public static class Producer {

        private String acks = "all";

        private Integer retries = 3;

        private Long batchSize = 16384L;

        private Long bufferMemory = 33554432L;

        private String defaultTopic = "arxiv-daily";

        private String compressionType = "none";

        private Long requestTimeoutMs = 30000L;

        private Long deliveryTimeoutMs = 120000L;

        private Boolean enableIdempotence = true;
    }
    
    /**
     * 消费者配置
     */
    @Data
    public static class Consumer {

        private String groupId = "arxiv-daily-group";

        private Boolean enableAutoCommit = true;

        private Long autoCommitIntervalMs = 5000L;

        private String autoOffsetReset = "latest";

        private Integer maxPollRecords = 500;

        private Long fetchMaxWaitMs = 500L;

        private Long sessionTimeoutMs = 30000L;

        private Long heartbeatIntervalMs = 10000L;

        private String[] topics = new String[]{"arxiv-daily"};

        private Integer concurrency = 1;

        private Boolean batchListener = false;
    }
}
