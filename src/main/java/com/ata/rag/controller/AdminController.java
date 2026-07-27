package com.ata.rag.controller;

import com.ata.rag.dto.AdminFeedbackResponse;
import com.ata.rag.dto.AdminQuestionsResponse;
import com.ata.rag.dto.SyncJobResponse;
import com.ata.rag.ingestion.pipeline.SyncJobService;
import com.ata.rag.model.CrawlRunEntity;
import com.ata.rag.repository.ChatAnalyticsRepository;
import com.ata.rag.repository.ChunkJdbcRepository;
import com.ata.rag.repository.CrawlRunRepository;
import com.ata.rag.repository.PageRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {
    private final SyncJobService syncJobService;
    private final PageRepository pageRepository;
    private final ChunkJdbcRepository chunkJdbcRepository;
    private final CrawlRunRepository crawlRunRepository;
    private final ChatAnalyticsRepository chatAnalyticsRepository;

    public AdminController(
            SyncJobService syncJobService,
            PageRepository pageRepository,
            ChunkJdbcRepository chunkJdbcRepository,
            CrawlRunRepository crawlRunRepository,
            ChatAnalyticsRepository chatAnalyticsRepository) {
        this.syncJobService = syncJobService;
        this.pageRepository = pageRepository;
        this.chunkJdbcRepository = chunkJdbcRepository;
        this.crawlRunRepository = crawlRunRepository;
        this.chatAnalyticsRepository = chatAnalyticsRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        ChatAnalyticsRepository.ChatSummary chatSummary = chatAnalyticsRepository.summary();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page_count", pageRepository.count());
        body.put("chunk_count", chunkJdbcRepository.countAll());
        body.put("chunks_by_source_type", chunkJdbcRepository.countBySourceType());
        body.put(
                "last_crawl",
                crawlRunRepository
                        .findFirstByRunTypeOrderByStartedAtDesc("website")
                        .map(this::serializeRun)
                        .orElse(null));
        body.put(
                "last_pricing_sync",
                crawlRunRepository
                        .findFirstByRunTypeOrderByStartedAtDesc("pricing")
                        .map(this::serializeRun)
                        .orElse(null));
        body.put("failed_page_count", pageRepository.countByStatus("failed"));
        body.put("avg_confidence", chatSummary.averageConfidence());
        body.put("avg_latency_ms", chatSummary.averageLatencyMs());
        body.put("total_questions", chatSummary.totalQuestions());
        body.put("answered_questions", chatSummary.answeredQuestions());
        body.put("unanswered_questions", chatSummary.unansweredQuestions());
        body.put("helpful_count", chatSummary.helpfulCount());
        body.put("not_helpful_count", chatSummary.notHelpfulCount());
        body.put("feedback_rate", chatSummary.feedbackRate());
        body.put("active_sync_job", syncJobService.activeJob());
        return body;
    }

    @GetMapping("/failed-pages")
    public Object failedPages() {
        return pageRepository.findByStatusIn(java.util.List.of("failed")).stream()
                .map(page -> Map.of(
                        "url", page.getUrl(),
                        "http_status", page.getHttpStatus() == null ? 0 : page.getHttpStatus(),
                        "error_message", page.getErrorMessage() == null ? "" : page.getErrorMessage()))
                .toList();
    }

    @GetMapping("/questions")
    public AdminQuestionsResponse questions(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return new AdminQuestionsResponse(
                chatAnalyticsRepository.topQuestions(limit).stream()
                        .map(item -> new AdminQuestionsResponse.TopQuestion(
                                item.question(), item.count()))
                        .toList(),
                chatAnalyticsRepository.unanswered(limit).stream()
                        .map(item -> new AdminQuestionsResponse.UnansweredQuestion(
                                item.question(), item.createdAt()))
                        .toList());
    }

    @GetMapping("/feedback")
    public AdminFeedbackResponse feedback(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        ChatAnalyticsRepository.ChatSummary chatSummary = chatAnalyticsRepository.summary();
        return new AdminFeedbackResponse(
                chatSummary.helpfulCount(),
                chatSummary.notHelpfulCount(),
                chatSummary.feedbackRate(),
                chatAnalyticsRepository.recentFeedback(limit).stream()
                        .map(item -> new AdminFeedbackResponse.FeedbackItem(
                                item.id(),
                                item.question(),
                                item.answer(),
                                item.helpful(),
                                item.createdAt(),
                                item.feedbackAt()))
                        .toList());
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SyncJobResponse sync() {
        return syncJobService
                .submitWebsite()
                .orElseThrow(() -> alreadyRunning("website"));
    }

    @PostMapping("/prices/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SyncJobResponse pricesSync() {
        return syncJobService
                .submitPricing()
                .orElseThrow(() -> alreadyRunning("pricing"));
    }

    private Map<String, Object> serializeRun(CrawlRunEntity run) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", run.getId().toString());
        body.put("run_type", run.getRunType());
        body.put("started_at", run.getStartedAt() == null ? null : run.getStartedAt().toString());
        body.put("finished_at", run.getFinishedAt() == null ? null : run.getFinishedAt().toString());
        body.put("status", run.getStatus());
        body.put("pages_discovered", run.getPagesDiscovered());
        body.put("pages_updated", run.getPagesUpdated());
        body.put("pages_failed", run.getPagesFailed());
        body.put("pages_removed", run.getPagesRemoved());
        body.put("error_summary", run.getErrorSummary());
        return body;
    }

    private ResponseStatusException alreadyRunning(String requestedJob) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot start " + requestedJob + " sync while "
                        + syncJobService.activeJob() + " is running");
    }
}
