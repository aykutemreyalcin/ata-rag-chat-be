package com.ata.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        String crawlBaseUrl,
        String pricingApiUrl,
        String openaiApiKey,
        String openaiModel,
        String embeddingModel,
        double confidenceThreshold,
        String basicAuthUser,
        String basicAuthPassword) {

    public RagProperties {
        if (crawlBaseUrl == null || crawlBaseUrl.isBlank()) {
            crawlBaseUrl = "https://akademiata.pl";
        }
        if (pricingApiUrl == null) {
            pricingApiUrl = "";
        }
        if (openaiApiKey == null) {
            openaiApiKey = "";
        }
        if (openaiModel == null || openaiModel.isBlank()) {
            openaiModel = "gpt-4.1-mini";
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = "text-embedding-3-small";
        }
        if (Double.isNaN(confidenceThreshold) || confidenceThreshold <= 0) {
            confidenceThreshold = 0.55;
        }
        if (basicAuthUser == null) {
            basicAuthUser = "";
        }
        if (basicAuthPassword == null) {
            basicAuthPassword = "";
        }
    }
}
