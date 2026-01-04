package com.mootann.arxivdaily.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mootann.arxivdaily.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka消息队列客户端
 * 统一封装Kafka操作，提供消息发送等功能
 */
@Slf4j
@Component
public class KafkaClient {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaConfig kafkaConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送消息（使用默认主题）
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean send(String message) {
        return send(kafkaConfig.getProducer().getDefaultTopic(), message);
    }

    /**
     * 发送消息
     * @param topic 主题名称
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean send(String topic, String message) {
        try {
            kafkaTemplate.send(topic, message);
            log.debug("发送消息成功: topic={}, message={}", topic, message);
            return true;
        } catch (Exception e) {
            log.error("发送消息失败: topic={}, message={}", topic, message, e);
            return false;
        }
    }

    /**
     * 发送带Key的消息
     * @param topic 主题名称
     * @param key 消息Key
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean send(String topic, String key, String message) {
        try {
            kafkaTemplate.send(topic, key, message);
            log.debug("发送消息成功: topic={}, key={}, message={}", topic, key, message);
            return true;
        } catch (Exception e) {
            log.error("发送消息失败: topic={}, key={}, message={}", topic, key, message, e);
            return false;
        }
    }

    /**
     * 发送对象消息（自动序列化为JSON）
     * @param topic 主题名称
     * @param obj 消息对象
     * @return 是否发送成功
     */
    public boolean sendObject(String topic, Object obj) {
    try {
        String jsonMessage = objectMapper.writeValueAsString(obj);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, jsonMessage);
        future.get(5, TimeUnit.SECONDS); // 等待发送结果
        return true;
    } catch (Exception e) {
        log.error("发送对象消息失败: topic={}, obj={}", topic, obj.getClass().getSimpleName(), e);
        return false;
    }
}

    /**
     * 发送对象消息（使用默认主题）
     * @param obj 消息对象
     * @return 是否发送成功
     */
    public boolean sendObject(Object obj) {
        return sendObject(kafkaConfig.getProducer().getDefaultTopic(), obj);
    }

    /**
     * 发送带Key的对象消息
     * @param topic 主题名称
     * @param key 消息Key
     * @param obj 消息对象
     * @return 是否发送成功
     */
    public boolean sendObject(String topic, String key, Object obj) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(obj);
            return send(topic, key, jsonMessage);
        } catch (Exception e) {
            log.error("发送对象消息失败: topic={}, key={}, obj={}", topic, key, obj.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * 异步发送消息（使用默认主题）
     * @param message 消息内容
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> sendAsync(String message) {
        return sendAsync(kafkaConfig.getProducer().getDefaultTopic(), message);
    }

    /**
     * 异步发送消息
     * @param topic 主题名称
     * @param message 消息内容
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> sendAsync(String topic, String message) {
        return kafkaTemplate.send(topic, message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("异步发送消息失败: topic={}, message={}", topic, message, ex);
                } else {
                    log.debug("异步发送消息成功: topic={}, partition={}, offset={}", 
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
    }

    /**
     * 异步发送带Key的消息
     * @param topic 主题名称
     * @param key 消息Key
     * @param message 消息内容
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> sendAsync(String topic, String key, String message) {
        return kafkaTemplate.send(topic, key, message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("异步发送消息失败: topic={}, key={}, message={}", topic, key, message, ex);
                } else {
                    log.debug("异步发送消息成功: topic={}, key={}, partition={}, offset={}", 
                        topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
    }

    /**
     * 异步发送对象消息
     * @param topic 主题名称
     * @param obj 消息对象
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> sendObjectAsync(String topic, Object obj) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(obj);
            return sendAsync(topic, jsonMessage);
        } catch (Exception e) {
            log.error("异步发送对象消息失败: topic={}, obj={}", topic, obj.getClass().getSimpleName(), e);
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 异步发送对象消息（使用默认主题）
     * @param obj 消息对象
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> sendObjectAsync(Object obj) {
        return sendObjectAsync(kafkaConfig.getProducer().getDefaultTopic(), obj);
    }

    /**
     * 批量发送消息
     * @param topic 主题名称
     * @param messages 消息列表
     * @return 成功发送的消息数量
     */
    public int sendBatch(String topic, List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("批量发送消息失败: 消息列表为空");
            return 0;
        }

        int successCount = 0;
        for (String message : messages) {
            if (send(topic, message)) {
                successCount++;
            }
        }
        log.info("批量发送消息完成: topic={}, total={}, success={}", topic, messages.size(), successCount);
        return successCount;
    }

    /**
     * 批量发送对象消息
     * @param topic 主题名称
     * @param objects 对象列表
     * @return 成功发送的消息数量
     */
    public int sendObjectBatch(String topic, List<?> objects) {
        if (objects == null || objects.isEmpty()) {
            log.warn("批量发送对象消息失败: 对象列表为空");
            return 0;
        }

        int successCount = 0;
        for (Object obj : objects) {
            if (sendObject(topic, obj)) {
                successCount++;
            }
        }
        log.info("批量发送对象消息完成: topic={}, total={}, success={}", topic, objects.size(), successCount);
        return successCount;
    }

    /**
     * 批量发送对象消息（使用默认主题）
     * @param objects 对象列表
     * @return 成功发送的消息数量
     */
    public int sendObjectBatch(List<?> objects) {
        return sendObjectBatch(kafkaConfig.getProducer().getDefaultTopic(), objects);
    }

    /**
     * 获取Kafka模板实例
     * @return KafkaTemplate
     */
    public KafkaTemplate<String, Object> getKafkaTemplate() {
        return kafkaTemplate;
    }

    /**
     * 获取默认主题名称
     * @return 主题名称
     */
    public String getDefaultTopic() {
        return kafkaConfig.getProducer().getDefaultTopic();
    }

    /**
     * 检查Kafka是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(kafkaConfig.getEnabled());
    }

    /**
     * 获取消费者组ID
     * @return 消费者组ID
     */
    public String getGroupId() {
        return kafkaConfig.getConsumer().getGroupId();
    }

    /**
     * 获取默认消费者主题列表
     * @return 主题数组
     */
    public String[] getDefaultTopics() {
        return kafkaConfig.getConsumer().getTopics();
    }
}
