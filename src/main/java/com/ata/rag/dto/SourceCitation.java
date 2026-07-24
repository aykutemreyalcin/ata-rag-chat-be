package com.ata.rag.dto;

public record SourceCitation(
        String title,
        String url,
        String section,
        String sourceType,
        double score) {}
