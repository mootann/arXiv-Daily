package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.repository.dto.ApiResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/zhipu")
public class ZhipuAiController {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @GetMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(@RequestParam String message) {
        log.info("Zhipu AI Chat Request: {}", message);
        String response = chatLanguageModel.generate(message);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/embedding")
    public ResponseEntity<ApiResponse<List<Float>>> embedding(@RequestBody String text) {
        log.info("Zhipu AI Embedding Request");
        Embedding embedding = embeddingModel.embed(text).content();
        return ResponseEntity.ok(ApiResponse.success(embedding.vectorAsList()));
    }
}
