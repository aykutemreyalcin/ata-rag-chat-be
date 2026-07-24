package com.ata.rag.chat;

import com.ata.rag.generation.GenerationResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ChatMetrics {
    private final MeterRegistry registry;
    private final DistributionSummary confidence;

    public ChatMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.confidence = DistributionSummary.builder("rag.chat.confidence")
                .description("RAG retrieval confidence")
                .baseUnit("ratio")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void record(
            boolean answered,
            double confidenceValue,
            long latencyMs,
            GenerationResult generation) {
        String outcome = answered ? "answered" : "unanswered";
        Counter.builder("rag.chat.requests")
                .description("RAG chat request outcomes")
                .tag("outcome", outcome)
                .tag("model", generation.model())
                .register(registry)
                .increment();
        Timer.builder("rag.chat.latency")
                .description("End-to-end RAG chat latency")
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(2))
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
        confidence.record(confidenceValue);
        recordTokens(generation.model(), "prompt", generation.promptTokens());
        recordTokens(generation.model(), "output", generation.outputTokens());
    }

    public void recordFailure(long latencyMs) {
        Counter.builder("rag.chat.requests")
                .description("RAG chat request outcomes")
                .tag("outcome", "failed")
                .tag("model", "unknown")
                .register(registry)
                .increment();
        Timer.builder("rag.chat.latency")
                .description("End-to-end RAG chat latency")
                .tag("outcome", "failed")
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    private void recordTokens(String model, String type, Integer count) {
        if (count == null || count <= 0) {
            return;
        }
        Counter.builder("rag.chat.tokens")
                .description("LLM token usage")
                .tag("model", model)
                .tag("type", type)
                .register(registry)
                .increment(count);
    }
}
