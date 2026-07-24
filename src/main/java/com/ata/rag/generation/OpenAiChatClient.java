package com.ata.rag.generation;

import com.ata.rag.config.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class OpenAiChatClient {
    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiChatClient(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public GenerationResult generate(String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.openaiModel(),
                    "temperature", 0,
                    "messages", List.of(Map.of("role", "user", "content", prompt))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            properties.openaiBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + properties.openaiApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "OpenAI-compatible chat failed: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");
            if (text.isBlank()) {
                throw new IllegalStateException("OpenAI-compatible chat returned an empty answer");
            }
            return new GenerationResult(text.strip(), properties.openaiModel());
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI-compatible chat request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI-compatible chat request interrupted", exception);
        }
    }
}
