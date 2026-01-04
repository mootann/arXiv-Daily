package com.mootann.arxivdaily.controller;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;

    // 存储每个会话的对话历史
    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionHistory.put(session.getId(), new ArrayList<>());
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionHistory.remove(session.getId());
        log.info("WebSocket connection closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message from {}: {}", session.getId(), payload);

        if (payload == null || payload.trim().isEmpty()) {
            log.warn("Empty payload received from {}", session.getId());
            return;
        }

        List<ChatMessage> history = sessionHistory.get(session.getId());
        if (history == null) {
            history = new ArrayList<>();
            sessionHistory.put(session.getId(), history);
        }

        // 添加用户消息到历史
        history.add(UserMessage.from(payload));

        // 构建完整响应内容，用于后续添加到历史
        StringBuilder fullResponseBuilder = new StringBuilder();

        List<ChatMessage> finalHistory = history;
        try {
            streamingChatLanguageModel.generate(history, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    if (token == null) {
                        return;
                    }
                    log.debug("Received token for session {}: {}", session.getId(), token);
                    try {
                        if (session.isOpen()) {
                            log.info("Sending token to client, session: {}, token length: {}", session.getId(), token.length());
                            session.sendMessage(new TextMessage(token));
                            fullResponseBuilder.append(token);
                            log.info("Token sent successfully");
                        } else {
                            log.warn("WebSocket session is closed, cannot send token");
                        }
                    } catch (IOException e) {
                        log.error("Error sending message to client, session: {}", session.getId(), e);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    // 将完整的 AI 响应添加到历史
                    String fullResponse = fullResponseBuilder.toString();
                    if (fullResponse.isEmpty()) {
                         if (response.content() != null && response.content().text() != null) {
                             fullResponse = response.content().text();
                             log.warn("Full response builder is empty, but response content is not. Using response content.");
                             // If builder was empty but we have content, we should probably send it?
                             // But onNext should have handled it.
                             // If onNext wasn't called, we might want to send the full response here as a fallback.
                             try {
                                 if (session.isOpen()) {
                                     session.sendMessage(new TextMessage(fullResponse));
                                 }
                             } catch (IOException e) {
                                 log.error("Error sending full response fallback", e);
                             }
                         } else {
                             log.warn("Streaming completed with empty response for session: {}", session.getId());
                         }
                    }
                    
                    finalHistory.add(AiMessage.from(fullResponse));
                    log.info("Streaming completed for session: {}. Response length: {}", session.getId(), fullResponse.length());
                    
                    // 发送结束标记，让前端知道流结束了
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage("[DONE]"));
                        }
                    } catch (IOException e) {
                        log.error("Error sending done signal", e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Error during streaming", error);
                    try {
                        if (session.isOpen()) {
                            String errorMessage = error.getMessage();
                            if (errorMessage == null) {
                                errorMessage = error.getClass().getName();
                            }
                            // 尝试获取更详细的原因
                            if (error.getCause() != null) {
                                errorMessage += " (Cause: " + error.getCause().getMessage() + ")";
                            }
                            
                            session.sendMessage(new TextMessage("Error: " + errorMessage));
                            // 发送部分堆栈信息以便调试（可选，生产环境应关闭）
                            for (StackTraceElement element : error.getStackTrace()) {
                                if (element.getClassName().contains("mootann") || element.getClassName().contains("langchain4j")) {
                                    session.sendMessage(new TextMessage("DEBUG: " + element.toString()));
                                }
                            }
                            session.sendMessage(new TextMessage("[DONE]"));
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error initiating streaming", e);
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
            session.sendMessage(new TextMessage("[DONE]"));
        }
    }
}
