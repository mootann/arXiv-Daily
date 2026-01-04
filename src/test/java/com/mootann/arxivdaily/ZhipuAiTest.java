package com.mootann.arxivdaily;

import com.mootann.arxivdaily.controller.ZhipuAiController;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 智谱AI模型集成测试
 * 用于验证ChatLanguageModel和EmbeddingModel的Bean创建和基本功能
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "zhipu.api-key=",
    "zhipu.model=glm-4.7",
    "zhipu.embedding-model=embedding-3-pro"
})
class ZhipuAiTest {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ZhipuAiController zhipuAiController;

    /**
     * 测试Spring容器是否成功加载智谱AI相关的Bean
     */
    @Test
    void testBeansAreCreated() {
        // 验证ChatLanguageModel的Bean是否创建
        assertThat(chatLanguageModel).isNotNull();
        log.info("✓ ChatLanguageModel Bean创建成功");
        
        // 验证EmbeddingModel的Bean是否创建
        assertThat(embeddingModel).isNotNull();
        log.info("✓ EmbeddingModel Bean创建成功");
        
        // 验证Controller的Bean是否创建
        assertThat(zhipuAiController).isNotNull();
        log.info("✓ ZhipuAiController Bean创建成功");
        
        log.info("==========================================");
        log.info("所有智谱AI相关的Bean已成功加载!");
        log.info("==========================================");
    }

    /**
     * 测试ChatLanguageModel的基本对话功能
     * 注意：此测试需要有效的API密钥和网络连接
     */
    @Test
    void testChatModelGeneration() {
        try {
            log.info("开始测试ChatLanguageModel对话功能...");
            
            // 发送简单的测试消息
            String testMessage = "你好，请用一句话介绍你自己。";
            log.info("发送测试消息: {}", testMessage);
            
            String response = chatLanguageModel.generate(testMessage);
            
            // 验证响应不为空
            assertThat(response).isNotNull();
            assertThat(response).isNotEmpty();
            
            log.info("✓ 收到响应: {}", response);
            log.info("✓ ChatLanguageModel测试通过");
            
        } catch (Exception e) {
            log.error("ChatLanguageModel测试失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试EmbeddingModel的向量化功能
     * 注意：此测试需要有效的API密钥和网络连接
     */
    @Test
    void testEmbeddingModelGeneration() {
        try {
            log.info("开始测试EmbeddingModel向量化功能...");
            
            // 测试文本
            String testText = "这是一个测试文本，用于生成向量嵌入。";
            log.info("测试文本: {}", testText);
            
            // 生成嵌入向量
            Response<Embedding> embeddingResponse = embeddingModel.embed(testText);
            Embedding embedding = embeddingResponse.content();
            
            // 验证嵌入向量不为空
            assertThat(embedding).isNotNull();
            assertThat(embedding.vector()).isNotEmpty();
            
            // 打印向量维度
            int dimension = embedding.vector().length;
            log.info("✓ 生成的向量维度: {}", dimension);
            log.info("✓ 向量前5个值: {}", 
                java.util.Arrays.toString(
                    java.util.Arrays.copyOf(embedding.vector(), Math.min(5, dimension))
                )
            );
            log.info("✓ EmbeddingModel测试通过");
            
        } catch (Exception e) {
            log.error("EmbeddingModel测试失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试向量相似度计算
     * 验证相似文本生成的向量之间的余弦相似度
     */
    @Test
    void testEmbeddingSimilarity() {
        try {
            log.info("开始测试向量相似度计算...");
            
            String text1 = "机器学习是人工智能的一个分支";
            String text2 = "深度学习是机器学习的一种方法";
            String text3 = "今天天气很好";
            
            // 生成三个文本的向量
            Embedding embedding1 = embeddingModel.embed(text1).content();
            Embedding embedding2 = embeddingModel.embed(text2).content();
            Embedding embedding3 = embeddingModel.embed(text3).content();
            
            // 计算余弦相似度
            double similarity12 = cosineSimilarity(embedding1.vector(), embedding2.vector());
            double similarity13 = cosineSimilarity(embedding1.vector(), embedding3.vector());
            
            log.info("文本1: {}", text1);
            log.info("文本2: {}", text2);
            log.info("文本3: {}", text3);
            log.info("文本1和文本2的相似度: {}", similarity12);
            log.info("文本1和文本3的相似度: {}", similarity13);
            
            // 验证相关文本的相似度应该高于不相关文本
            assertThat(similarity12).isGreaterThan(similarity13);
            log.info("✓ 相似度计算测试通过");
            
        } catch (Exception e) {
            log.error("相似度计算测试失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 计算两个向量的余弦相似度
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 余弦相似度值（-1到1之间）
     */
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
