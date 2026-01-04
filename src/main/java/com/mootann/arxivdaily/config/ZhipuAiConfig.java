package com.mootann.arxivdaily.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ZhipuAiConfig {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("${zhipu.model:glm-4.7}")
    private String modelName;

    @Value("${zhipu.embedding-model:embedding-3-pro}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel zhipuAiChatModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(modelName)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .callTimeout(Duration.ofSeconds(60))      // API调用超时时间
                .connectTimeout(Duration.ofSeconds(10))   // 连接超时时间
                .readTimeout(Duration.ofSeconds(60))      // 读取超时时间
                .writeTimeout(Duration.ofSeconds(60))     // 写入超时时间
                .build();
    }

    @Bean
    public EmbeddingModel zhipuAiEmbeddingModel() {
        return ZhipuAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .model(embeddingModelName)
                .logRequests(true)
                .logResponses(true)
                .callTimeout(Duration.ofSeconds(60))      // API调用超时时间
                .connectTimeout(Duration.ofSeconds(10))   // 连接超时时间
                .readTimeout(Duration.ofSeconds(60))      // 读取超时时间
                .writeTimeout(Duration.ofSeconds(60))     // 写入超时时间
                .build();
    }
}
