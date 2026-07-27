package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChatFeedbackRequest(
        @NotNull
                @JsonProperty("query_id")
                @JsonAlias("queryId")
                UUID queryId,
        @NotNull Boolean helpful) {}
