package com.ata.rag.controller;

import com.ata.rag.dto.ChatFeedbackRequest;
import com.ata.rag.model.ChatQueryEntity;
import com.ata.rag.repository.ChatQueryRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
public class ChatFeedbackController {
    private final ChatQueryRepository chatQueryRepository;

    public ChatFeedbackController(ChatQueryRepository chatQueryRepository) {
        this.chatQueryRepository = chatQueryRepository;
    }

    @PostMapping("/feedback")
    public Map<String, Object> feedback(@Valid @RequestBody ChatFeedbackRequest request) {
        ChatQueryEntity query = chatQueryRepository
                .findById(request.queryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat query not found"));
        query.setHelpful(request.helpful());
        query.setFeedbackAt(Instant.now());
        chatQueryRepository.save(query);
        return Map.of(
                "query_id", query.getId().toString(),
                "helpful", query.getHelpful(),
                "feedback_at", query.getFeedbackAt().toString());
    }
}
