package com.ata.rag.retrieval;

import java.util.List;

public record RetrievalResult(List<RetrievedChunk> chunks, double confidence) {
    public boolean answered(double threshold) {
        return !chunks.isEmpty() && confidence >= threshold;
    }
}
