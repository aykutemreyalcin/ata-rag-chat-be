package com.ata.rag.retrieval;

import java.util.UUID;

public record RetrievedChunk(
        UUID id,
        String content,
        String section,
        String url,
        String title,
        String sourceType,
        double score) {}
