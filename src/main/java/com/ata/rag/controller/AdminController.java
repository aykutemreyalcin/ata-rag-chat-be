package com.ata.rag.controller;

import com.ata.rag.ingestion.pipeline.WebsiteSyncService;
import com.ata.rag.ingestion.pricing.PricingSyncService;
import com.ata.rag.model.CrawlRunEntity;
import com.ata.rag.repository.ChunkJdbcRepository;
import com.ata.rag.repository.CrawlRunRepository;
import com.ata.rag.repository.PageRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final WebsiteSyncService websiteSyncService;
    private final PricingSyncService pricingSyncService;
    private final PageRepository pageRepository;
    private final ChunkJdbcRepository chunkJdbcRepository;
    private final CrawlRunRepository crawlRunRepository;

    public AdminController(
            WebsiteSyncService websiteSyncService,
            PricingSyncService pricingSyncService,
            PageRepository pageRepository,
            ChunkJdbcRepository chunkJdbcRepository,
            CrawlRunRepository crawlRunRepository) {
        this.websiteSyncService = websiteSyncService;
        this.pricingSyncService = pricingSyncService;
        this.pageRepository = pageRepository;
        this.chunkJdbcRepository = chunkJdbcRepository;
        this.crawlRunRepository = crawlRunRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
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
        body.put("avg_confidence", null);
        body.put("avg_latency_ms", null);
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
    public void questions() {
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Admin questions requires chat_queries from branch be/rag-chat-api / be/admin-observability.");
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        CrawlRunEntity run = websiteSyncService.sync();
        return serializeRun(run);
    }

    @PostMapping("/prices/sync")
    public Map<String, Object> pricesSync() {
        CrawlRunEntity run = pricingSyncService.sync();
        return serializeRun(run);
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
}
