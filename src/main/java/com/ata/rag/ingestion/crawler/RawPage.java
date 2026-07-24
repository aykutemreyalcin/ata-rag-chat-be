package com.ata.rag.ingestion.crawler;

public record RawPage(
        String url, int statusCode, String contentType, byte[] body, String lastModifiedHeader) {}
