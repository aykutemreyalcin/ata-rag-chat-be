package com.ata.rag.ingestion.chunking;

public record TextChunk(String section, String text, int tokenCount) {}
