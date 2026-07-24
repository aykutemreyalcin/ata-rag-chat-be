package com.ata.rag.controller;

import com.ata.rag.dto.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chat API scaffold. Full SSE RAG implementation lands in branch {@code be/rag-chat-api}.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    @PostMapping("/chat")
    public void chat(@Valid @RequestBody ChatRequest request) {
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Chat RAG is not implemented yet. See branch be/rag-chat-api and docs/openapi.yaml.");
    }
}
