package com.ata.rag.ingestion.pipeline;

import com.ata.rag.config.RagProperties;
import com.ata.rag.ingestion.crawler.RawPage;
import com.ata.rag.ingestion.crawler.WebsiteCrawler;
import com.ata.rag.model.CrawlRunEntity;
import com.ata.rag.model.PageEntity;
import com.ata.rag.repository.ChunkJdbcRepository;
import com.ata.rag.repository.CrawlRunRepository;
import com.ata.rag.repository.PageRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebsiteSyncService {
    private static final Logger log = LoggerFactory.getLogger(WebsiteSyncService.class);
    private static final int MAX_CONSECUTIVE_MISSES = 3;

    private final RagProperties properties;
    private final PageProcessor pageProcessor;
    private final PageIngestService pageIngestService;
    private final PageRepository pageRepository;
    private final CrawlRunRepository crawlRunRepository;
    private final ChunkJdbcRepository chunkJdbcRepository;

    public WebsiteSyncService(
            RagProperties properties,
            PageProcessor pageProcessor,
            PageIngestService pageIngestService,
            PageRepository pageRepository,
            CrawlRunRepository crawlRunRepository,
            ChunkJdbcRepository chunkJdbcRepository) {
        this.properties = properties;
        this.pageProcessor = pageProcessor;
        this.pageIngestService = pageIngestService;
        this.pageRepository = pageRepository;
        this.crawlRunRepository = crawlRunRepository;
        this.chunkJdbcRepository = chunkJdbcRepository;
    }

    public CrawlRunEntity sync() {
        WebsiteCrawler crawler = new WebsiteCrawler(
                properties.crawlBaseUrl(), properties.userAgent(), properties.crawlTimeoutSeconds());
        return sync(crawler);
    }

    public CrawlRunEntity sync(WebsiteCrawler crawler) {
        CrawlRunEntity run = new CrawlRunEntity();
        run.setRunType("website");
        run.setStartedAt(Instant.now());
        run.setStatus("running");
        run = crawlRunRepository.save(run);

        Set<String> discovered = new HashSet<>();
        int updated = 0;
        int failed = 0;

        try {
            for (RawPage rawPage : crawlSite(crawler)) {
                discovered.add(rawPage.url());
                try {
                    if ("other".equals(rawPage.contentType())) {
                        continue;
                    }
                    ProcessedPage processed = pageProcessor.process(rawPage);
                    pageIngestService.ingest(processed);
                    updated++;
                } catch (Exception exception) {
                    log.warn("Failed to ingest {}: {}", rawPage.url(), exception.toString());
                    recordFailedPage(rawPage, exception);
                    failed++;
                }
            }
            int removed = cleanupStalePages(discovered);
            run.setStatus(failed == 0 ? "success" : "partial");
            run.setPagesDiscovered(discovered.size());
            run.setPagesUpdated(updated);
            run.setPagesFailed(failed);
            run.setPagesRemoved(removed);
        } catch (Exception exception) {
            log.error("Website sync failed", exception);
            run.setStatus("failed");
            run.setErrorSummary(truncate(exception.getMessage()));
        } finally {
            run.setFinishedAt(Instant.now());
            crawlRunRepository.save(run);
        }
        return run;
    }

    private Iterable<RawPage> crawlSite(WebsiteCrawler crawler) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(crawler.baseUrl());
        List<RawPage> pages = new java.util.ArrayList<>();
        int maxPages = properties.crawlMaxPages();

        while (!queue.isEmpty()) {
            if (maxPages > 0 && pages.size() >= maxPages) {
                break;
            }
            String url = queue.poll();
            if (!visited.add(url)) {
                continue;
            }
            try {
                RawPage page = crawler.fetch(url);
                pages.add(page);
                if ("html".equals(page.contentType())) {
                    for (String link : crawler.discoverLinks(page)) {
                        if (!visited.contains(link)) {
                            queue.add(link);
                        }
                    }
                }
            } catch (Exception exception) {
                log.warn("Failed to fetch {}: {}", url, exception.toString());
            }
        }
        return pages;
    }

    @Transactional
    protected void recordFailedPage(RawPage rawPage, Exception error) {
        Instant now = Instant.now();
        String sourceType = "pdf".equals(rawPage.contentType()) ? "pdf" : "html";
        PageEntity page = pageRepository.findByUrl(rawPage.url()).orElseGet(PageEntity::new);
        page.setUrl(rawPage.url());
        page.setSourceType(sourceType);
        if (page.getContentHash() == null) {
            page.setContentHash("");
        }
        page.setLastCrawledAt(now);
        page.setStatus("failed");
        page.setHttpStatus(rawPage.statusCode());
        page.setErrorMessage(truncate(error.getMessage()));
        pageRepository.save(page);
    }

    @Transactional
    protected int cleanupStalePages(Set<String> discoveredUrls) {
        List<PageEntity> pages = pageRepository.findByStatusIn(List.of("active", "failed")).stream()
                .filter(page -> !"pricing".equals(page.getSourceType()))
                .toList();
        int removed = 0;
        for (PageEntity page : pages) {
            if (discoveredUrls.contains(page.getUrl())) {
                page.setConsecutiveMissCount(0);
                pageRepository.save(page);
                continue;
            }
            page.setConsecutiveMissCount(page.getConsecutiveMissCount() + 1);
            if (page.getConsecutiveMissCount() > MAX_CONSECUTIVE_MISSES) {
                chunkJdbcRepository.deleteByPageId(page.getId());
                pageRepository.delete(page);
                removed++;
            } else {
                pageRepository.save(page);
            }
        }
        return removed;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
