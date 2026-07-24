package com.ata.rag.ingestion.pipeline;

import com.ata.rag.dto.SyncJobResponse;
import com.ata.rag.ingestion.pricing.PricingSyncService;
import com.ata.rag.model.CrawlRunEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SyncJobService {
    private static final Logger log = LoggerFactory.getLogger(SyncJobService.class);

    private final WebsiteSyncService websiteSyncService;
    private final PricingSyncService pricingSyncService;
    private final MeterRegistry meterRegistry;
    private final AtomicReference<String> activeJob = new AtomicReference<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SyncJobService(
            WebsiteSyncService websiteSyncService,
            PricingSyncService pricingSyncService,
            MeterRegistry meterRegistry) {
        this.websiteSyncService = websiteSyncService;
        this.pricingSyncService = pricingSyncService;
        this.meterRegistry = meterRegistry;
    }

    public Optional<SyncJobResponse> submitWebsite() {
        return submit("website", websiteSyncService::sync);
    }

    public Optional<SyncJobResponse> submitPricing() {
        return submit("pricing", pricingSyncService::sync);
    }

    public void runNightly() {
        if (!activeJob.compareAndSet(null, "nightly")) {
            recordCounter("nightly", "rejected");
            log.warn("ingest.job.rejected job=nightly activeJob={}", activeJob.get());
            return;
        }
        try {
            runMeasured("website", websiteSyncService::sync);
            runMeasured("pricing", pricingSyncService::sync);
        } finally {
            activeJob.set(null);
        }
    }

    public String activeJob() {
        return activeJob.get();
    }

    private Optional<SyncJobResponse> submit(
            String job, Supplier<CrawlRunEntity> task) {
        if (!activeJob.compareAndSet(null, job)) {
            recordCounter(job, "rejected");
            log.warn("ingest.job.rejected job={} activeJob={}", job, activeJob.get());
            return Optional.empty();
        }
        Instant submittedAt = Instant.now();
        recordCounter(job, "accepted");
        executor.submit(() -> {
            try {
                runMeasured(job, task);
            } finally {
                activeJob.compareAndSet(job, null);
            }
        });
        return Optional.of(new SyncJobResponse(job, "accepted", submittedAt));
    }

    private void runMeasured(String job, Supplier<CrawlRunEntity> task) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        log.info("ingest.job.start job={}", job);
        try {
            CrawlRunEntity run = task.get();
            if (run != null && run.getStatus() != null && !run.getStatus().isBlank()) {
                outcome = run.getStatus();
            }
        } catch (RuntimeException exception) {
            outcome = "failed";
            log.error("ingest.job.failed job={}", job, exception);
            throw exception;
        } finally {
            sample.stop(Timer.builder("rag.ingest.job.duration")
                    .description("Ingestion job duration")
                    .tag("job", job)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            recordCounter(job, outcome);
            log.info("ingest.job.finish job={} outcome={}", job, outcome);
        }
    }

    private void recordCounter(String job, String outcome) {
        Counter.builder("rag.ingest.jobs")
                .description("Ingestion job outcomes")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    @PreDestroy
    void closeExecutor() {
        executor.close();
    }
}
