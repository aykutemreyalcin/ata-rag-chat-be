package com.ata.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crawl_runs")
public class CrawlRunEntity {

    @Id
    private UUID id;

    @Column(name = "run_type", nullable = false, length = 16)
    private String runType = "website";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "pages_discovered", nullable = false)
    private int pagesDiscovered;

    @Column(name = "pages_updated", nullable = false)
    private int pagesUpdated;

    @Column(name = "pages_failed", nullable = false)
    private int pagesFailed;

    @Column(name = "pages_removed", nullable = false)
    private int pagesRemoved;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPagesDiscovered() {
        return pagesDiscovered;
    }

    public void setPagesDiscovered(int pagesDiscovered) {
        this.pagesDiscovered = pagesDiscovered;
    }

    public int getPagesUpdated() {
        return pagesUpdated;
    }

    public void setPagesUpdated(int pagesUpdated) {
        this.pagesUpdated = pagesUpdated;
    }

    public int getPagesFailed() {
        return pagesFailed;
    }

    public void setPagesFailed(int pagesFailed) {
        this.pagesFailed = pagesFailed;
    }

    public int getPagesRemoved() {
        return pagesRemoved;
    }

    public void setPagesRemoved(int pagesRemoved) {
        this.pagesRemoved = pagesRemoved;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }
}
