package com.ata.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        String crawlBaseUrl,
        String pricingApiUrl,
        String pricingCitationUrl,
        String openaiApiKey,
        String openaiModel,
        String embeddingModel,
        int embeddingDimensions,
        double confidenceThreshold,
        String basicAuthUser,
        String basicAuthPassword,
        int crawlMaxPages,
        int crawlTimeoutSeconds,
        boolean schedulerEnabled,
        String userAgent) {

    public RagProperties {
        if (crawlBaseUrl == null || crawlBaseUrl.isBlank()) {
            crawlBaseUrl = "https://akademiata.pl";
        }
        if (pricingApiUrl == null) {
            pricingApiUrl = "";
        }
        if (pricingCitationUrl == null || pricingCitationUrl.isBlank()) {
            pricingCitationUrl = "https://akademiata.pl/kalkulator-czesnego/";
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
        if (embeddingDimensions <= 0) {
            embeddingDimensions = 1536;
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
        if (crawlMaxPages < 0) {
            crawlMaxPages = 0;
        }
        if (crawlTimeoutSeconds <= 0) {
            crawlTimeoutSeconds = 20;
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "AkademiaTA-RAG-Bot/1.0";
        }
    }

    public boolean hasOpenAiKey() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }
}
