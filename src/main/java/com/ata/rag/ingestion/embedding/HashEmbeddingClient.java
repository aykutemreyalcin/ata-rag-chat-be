package com.ata.rag.ingestion.embedding;

import com.ata.rag.config.RagProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class HashEmbeddingClient implements EmbeddingClient {
    private final int dimensions;

    public HashEmbeddingClient(RagProperties properties) {
        this.dimensions = properties.embeddingDimensions();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embedOne(text));
        }
        return vectors;
    }

    private float[] embedOne(String text) {
        float[] vector = new float[dimensions];
        byte[] seed = sha256(text == null ? "" : text);
        for (int i = 0; i < dimensions; i++) {
            int b = seed[i % seed.length] & 0xff;
            int b2 = seed[(i * 7) % seed.length] & 0xff;
            vector[i] = ((b / 255f) * 2f - 1f) * 0.7f + ((b2 / 255f) * 2f - 1f) * 0.3f;
        }
        normalize(vector);
        return vector;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
