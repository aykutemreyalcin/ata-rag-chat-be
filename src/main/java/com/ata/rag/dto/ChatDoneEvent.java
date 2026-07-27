package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ChatDoneEvent(
        @JsonProperty("query_id") UUID queryId,
        double confidence,
        boolean answered,
        @JsonProperty("source_count") int sourceCount,
        @JsonProperty("latency_ms") long latencyMs,
        String model) {}
