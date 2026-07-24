package com.ata.rag.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ata.rag.generation.GenerationResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ChatMetricsTest {

    @Test
    void recordsOutcomeLatencyConfidenceAndTokens() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatMetrics metrics = new ChatMetrics(registry);

        metrics.record(
                true,
                0.88,
                250,
                new GenerationResult("answer", "test-model", 100, 20));

        assertEquals(
                1,
                registry.get("rag.chat.requests")
                        .tag("outcome", "answered")
                        .counter()
                        .count());
        assertEquals(
                100,
                registry.get("rag.chat.tokens")
                        .tag("type", "prompt")
                        .counter()
                        .count());
        assertEquals(
                20,
                registry.get("rag.chat.tokens")
                        .tag("type", "output")
                        .counter()
                        .count());
        assertEquals(1, registry.get("rag.chat.latency").timer().count());
        assertEquals(1, registry.get("rag.chat.confidence").summary().count());
    }
}
