package com.ata.rag.ingestion.scheduler;

import com.ata.rag.ingestion.pipeline.SyncJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class IngestScheduler {
    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);

    private final SyncJobService syncJobService;

    public IngestScheduler(SyncJobService syncJobService) {
        this.syncJobService = syncJobService;
    }

    @Scheduled(cron = "${INGEST_CRON:0 0 3 * * *}")
    public void nightlySync() {
        log.info("Starting scheduled website + pricing ingest");
        syncJobService.runNightly();
    }
}
