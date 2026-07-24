package com.ata.rag.controller;

import com.ata.rag.chat.ChatService;
import com.ata.rag.dto.ChatRequest;
import com.ata.rag.security.QuestionSanitizer;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;
    private final QuestionSanitizer questionSanitizer;

    public ChatController(ChatService chatService, QuestionSanitizer questionSanitizer) {
        this.chatService = chatService;
        this.questionSanitizer = questionSanitizer;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        String question = questionSanitizer.sanitize(request.question());
        return chatService.stream(question, request.topK());
    }
}
