package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record SyncJobResponse(
        String job,
        String status,
        @JsonProperty("submitted_at") Instant submittedAt) {}
