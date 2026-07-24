package com.ata.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vertex-ai")
public record VertexAiProperties(
        boolean enabled,
        String projectId,
        String region,
        String modelName,
        String credentialsPath) {

    public VertexAiProperties {
        if (projectId == null) {
            projectId = "";
        }
        if (region == null || region.isBlank()) {
            region = "europe-west1";
        }
        if (modelName == null || modelName.isBlank()) {
            modelName = "gemini-2.5-flash";
        }
        if (credentialsPath == null) {
            credentialsPath = "";
        }
    }
}
