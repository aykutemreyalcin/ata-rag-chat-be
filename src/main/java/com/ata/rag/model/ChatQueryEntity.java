package com.ata.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_queries")
public class ChatQueryEntity {
    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private boolean answered;

    private Double confidence;

    @Column(name = "retrieval_score")
    private Double retrievalScore;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "latency_ms")
    private Long latencyMs;

    private String model;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    private Boolean helpful;

    @Column(name = "feedback_at")
    private Instant feedbackAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getRetrievalScore() {
        return retrievalScore;
    }

    public void setRetrievalScore(Double retrievalScore) {
        this.retrievalScore = retrievalScore;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Boolean getHelpful() {
        return helpful;
    }

    public void setHelpful(Boolean helpful) {
        this.helpful = helpful;
    }

    public Instant getFeedbackAt() {
        return feedbackAt;
    }

    public void setFeedbackAt(Instant feedbackAt) {
        this.feedbackAt = feedbackAt;
    }
}
