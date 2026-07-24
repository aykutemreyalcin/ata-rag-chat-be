package com.ata.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String question,
        @JsonProperty("top_k") @Min(1) @Max(20) Integer topK) {

    public ChatRequest {
        if (topK == null) {
            topK = 5;
        }
    }
}
