package com.ata.rag.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VertexAiProperties.class)
public class VertexAiConfig {
    private static final String CLOUD_PLATFORM_SCOPE =
            "https://www.googleapis.com/auth/cloud-platform";

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.vertex-ai", name = "enabled", havingValue = "true")
    VertexAI vertexAI(VertexAiProperties properties) throws IOException {
        if (properties.projectId().isBlank()) {
            throw new IllegalStateException("GCP_PROJECT_ID is required when Vertex AI is enabled");
        }
        VertexAI.Builder builder = new VertexAI.Builder()
                .setProjectId(properties.projectId())
                .setLocation(properties.region());
        if (!properties.credentialsPath().isBlank()) {
            Path path = Path.of(properties.credentialsPath()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Google credentials file not found: " + path);
            }
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(path.toFile()))
                    .createScoped(CLOUD_PLATFORM_SCOPE);
            builder.setCredentials(credentials);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.vertex-ai", name = "enabled", havingValue = "true")
    GenerativeModel generativeModel(VertexAI vertexAI, VertexAiProperties properties) {
        GenerationConfig generationConfig = GenerationConfig.newBuilder()
                .setTemperature(0.0f)
                .setTopP(0.8f)
                .setMaxOutputTokens(4096)
                .build();
        return new GenerativeModel.Builder()
                .setModelName(properties.modelName())
                .setVertexAi(vertexAI)
                .setGenerationConfig(generationConfig)
                .build();
    }
}
