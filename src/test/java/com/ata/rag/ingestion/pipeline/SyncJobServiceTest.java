package com.ata.rag.ingestion.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.ata.rag.ingestion.pricing.PricingSyncService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SyncJobServiceTest {

    @Test
    void rejectsOverlappingSyncJobs() throws Exception {
        WebsiteSyncService website = mock(WebsiteSyncService.class);
        PricingSyncService pricing = mock(PricingSyncService.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
                    started.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return null;
                })
                .when(website)
                .sync();

        SyncJobService jobs =
                new SyncJobService(website, pricing, new SimpleMeterRegistry());
        try {
            assertTrue(jobs.submitWebsite().isPresent());
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(jobs.submitPricing().isEmpty());
            assertEquals("website", jobs.activeJob());
        } finally {
            release.countDown();
            jobs.closeExecutor();
        }
    }
}
