package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminFeedbackResponse(
        @JsonProperty("helpful_count") long helpfulCount,
        @JsonProperty("not_helpful_count") long notHelpfulCount,
        @JsonProperty("feedback_rate") Double feedbackRate,
        List<FeedbackItem> recent) {

    public record FeedbackItem(
            UUID id,
            String question,
            String answer,
            Boolean helpful,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("feedback_at") Instant feedbackAt) {}
}
