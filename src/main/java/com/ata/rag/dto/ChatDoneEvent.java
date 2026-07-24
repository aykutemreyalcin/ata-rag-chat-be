package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatDoneEvent(
        double confidence,
        boolean answered,
        @JsonProperty("source_count") int sourceCount,
        @JsonProperty("latency_ms") long latencyMs,
        String model) {}
