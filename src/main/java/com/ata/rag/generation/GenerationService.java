package com.ata.rag.generation;

import com.ata.rag.config.RagProperties;
import com.ata.rag.config.VertexAiProperties;
import com.ata.rag.retrieval.RetrievedChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private final GenerativeModel vertexModel;
    private final VertexAiProperties vertexProperties;
    private final RagProperties ragProperties;
    private final OpenAiChatClient openAiChatClient;

    public GenerationService(
            ObjectProvider<GenerativeModel> vertexModelProvider,
            VertexAiProperties vertexProperties,
            RagProperties ragProperties,
            ObjectMapper objectMapper) {
        this.vertexModel = vertexModelProvider.getIfAvailable();
        this.vertexProperties = vertexProperties;
        this.ragProperties = ragProperties;
        this.openAiChatClient = new OpenAiChatClient(ragProperties, objectMapper);
    }

    public GenerationResult generate(String question, List<RetrievedChunk> chunks) {
        String prompt = GroundingPrompt.build(question, chunks);
        if (vertexModel == null && ragProperties.hasOpenAiKey()) {
            return openAiChatClient.generate(prompt);
        }
        if (vertexModel == null) {
            return new GenerationResult(extractiveAnswer(chunks), "extractive-fallback");
        }

        long startedNanos = System.nanoTime();
        try {
            GenerateContentResponse response = vertexModel.generateContent(prompt);
            String text = ResponseHandler.getText(response);
            log.info(
                    "rag.generation.success model={} latencyMs={} sourceCount={}",
                    vertexProperties.modelName(),
                    elapsedMs(startedNanos),
                    chunks.size());
            return new GenerationResult(text.strip(), vertexProperties.modelName());
        } catch (IOException exception) {
            log.warn(
                    "rag.generation.failed model={} latencyMs={} error={}",
                    vertexProperties.modelName(),
                    elapsedMs(startedNanos),
                    exception.getMessage());
            throw new IllegalStateException("Vertex AI generation failed", exception);
        }
    }

    private static String extractiveAnswer(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "I couldn't find this information on the AkademiaTA website.";
        }
        String content = chunks.getFirst().content();
        if (content == null || content.isBlank()) {
            return "I couldn't find this information on the AkademiaTA website.";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        int max = Math.min(compact.length(), 900);
        return compact.substring(0, max);
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
