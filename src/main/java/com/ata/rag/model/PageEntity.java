package com.ata.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pages")
public class PageEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "source_type", nullable = false, length = 16)
    private String sourceType;

    @Column(length = 8)
    private String language;

    @Column(name = "content_hash", nullable = false, columnDefinition = "TEXT")
    private String contentHash;

    @Column(name = "last_modified")
    private Instant lastModified;

    @Column(name = "last_crawled_at", nullable = false)
    private Instant lastCrawledAt;

    @Column(nullable = false, length = 16)
    private String status = "active";

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "consecutive_miss_count", nullable = false)
    private int consecutiveMissCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
        if (lastCrawledAt == null) {
            lastCrawledAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Instant getLastCrawledAt() {
        return lastCrawledAt;
    }

    public void setLastCrawledAt(Instant lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getConsecutiveMissCount() {
        return consecutiveMissCount;
    }

    public void setConsecutiveMissCount(int consecutiveMissCount) {
        this.consecutiveMissCount = consecutiveMissCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
