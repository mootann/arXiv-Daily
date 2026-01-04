package com.mootann.arxivdaily.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * 通用对象配置
 * 统一配置ObjectMapper等公共组件
 */
@Configuration
public class ObjectConfig {

    /**
     * 默认的ObjectMapper，用于HTTP请求等场景
     * 不启用多态类型处理
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册Java 8日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
    
    /**
     * Redis专用的ObjectMapper
     * 启用多态类型处理以支持抽象类型（如Page）
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册Java 8日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 允许缺少 creator，支持PageImpl等类型
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        // 注册 PageImpl 和 PageRequest 的 Mixin，解决反序列化问题
        objectMapper.addMixIn(PageImpl.class, PageMixin.class);
        objectMapper.addMixIn(PageRequest.class, PageRequestMixin.class);
        
        // 配置多态类型验证器，允许序列化抽象类型
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .build();
        
        objectMapper.activateDefaultTyping(
            ptv,
            ObjectMapper.DefaultTyping.NON_FINAL,
            com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        
        return objectMapper;
    }
    
    /**
     * PageImpl 的 Mixin，用于解决 Jackson 反序列化问题
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class PageMixin {
        @com.fasterxml.jackson.annotation.JsonCreator
        PageMixin(
            @JsonProperty("content") List<?> content,
            @JsonProperty("number") int number,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long totalElements
        ) {}
    }
    
    /**
     * PageRequest 的 Mixin，用于解决 Jackson 反序列化问题
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class PageRequestMixin {
        @com.fasterxml.jackson.annotation.JsonCreator
        PageRequestMixin(
            @JsonProperty("page") int page,
            @JsonProperty("size") int size
        ) {}
    }
}
