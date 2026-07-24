package com.ata.rag.ingestion.embedding;

import com.ata.rag.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingClient delegate;
    private final String model;

    public EmbeddingService(RagProperties properties, ObjectMapper objectMapper) {
        this.model = properties.embeddingModel();
        if (properties.hasOpenAiKey()) {
            log.info("Using OpenAI embeddings model={}", model);
            this.delegate = new OpenAiEmbeddingClient(properties, objectMapper);
        } else {
            log.warn("OPENAI_API_KEY missing — using deterministic hash embeddings for ingest");
            this.delegate = new HashEmbeddingClient(properties);
        }
    }

    public List<float[]> embed(List<String> texts) {
        return delegate.embed(texts);
    }

    public String model() {
        return model;
    }
}
