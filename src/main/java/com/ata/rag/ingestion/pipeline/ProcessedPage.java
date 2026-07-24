package com.ata.rag.ingestion.pipeline;

public record ProcessedPage(
        String url,
        String title,
        String markdown,
        String language,
        String sourceType,
        String contentHash,
        int httpStatus) {}
