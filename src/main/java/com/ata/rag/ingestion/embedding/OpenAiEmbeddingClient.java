package com.ata.rag.ingestion.embedding;

import com.ata.rag.config.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAiEmbeddingClient implements EmbeddingClient {
    private static final int BATCH_SIZE = 64;

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiEmbeddingClient(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        List<float[]> embeddings = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            List<String> batch = texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()));
            embeddings.addAll(embedBatch(batch));
        }
        return embeddings;
    }

    private List<float[]> embedBatch(List<String> batch) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.embeddingModel(),
                    "input", batch));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            properties.openaiBaseUrl().replaceAll("/+$", "") + "/embeddings"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + properties.openaiApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI embeddings failed: HTTP " + response.statusCode() + " "
                        + truncate(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            float[][] ordered = new float[batch.size()][];
            for (JsonNode item : data) {
                int index = item.path("index").asInt();
                JsonNode embeddingNode = item.path("embedding");
                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                ordered[index] = vector;
            }
            List<float[]> result = new ArrayList<>(batch.size());
            for (float[] vector : ordered) {
                if (vector == null) {
                    throw new IllegalStateException("Missing embedding in OpenAI response");
                }
                result.add(vector);
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI embedding request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI embedding request interrupted", exception);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) : body;
    }
}
