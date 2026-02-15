package com.mootann.arxivdaily.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> chatStream(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        String context = payload.get("context");
        String prompt = message;
        if (context != null && !context.isEmpty()) {
            prompt = "Context: " + context + "\n\nQuestion: " + message;
        }
        
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
