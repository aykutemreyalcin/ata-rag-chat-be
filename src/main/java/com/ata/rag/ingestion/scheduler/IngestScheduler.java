package com.ata.rag.ingestion.scheduler;

import com.ata.rag.config.RagProperties;
import com.ata.rag.ingestion.pipeline.WebsiteSyncService;
import com.ata.rag.ingestion.pricing.PricingSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class IngestScheduler {
    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);

    private final WebsiteSyncService websiteSyncService;
    private final PricingSyncService pricingSyncService;

    public IngestScheduler(WebsiteSyncService websiteSyncService, PricingSyncService pricingSyncService) {
        this.websiteSyncService = websiteSyncService;
        this.pricingSyncService = pricingSyncService;
    }

    @Scheduled(cron = "${INGEST_CRON:0 0 3 * * *}")
    public void nightlySync() {
        log.info("Starting scheduled website + pricing ingest");
        websiteSyncService.sync();
        pricingSyncService.sync();
    }
}
