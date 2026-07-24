package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record AdminQuestionsResponse(
        @JsonProperty("top_questions") List<TopQuestion> topQuestions,
        List<UnansweredQuestion> unanswered) {

    public record TopQuestion(String question, long count) {}

    public record UnansweredQuestion(
            String question,
            @JsonProperty("created_at") Instant createdAt) {}
}
