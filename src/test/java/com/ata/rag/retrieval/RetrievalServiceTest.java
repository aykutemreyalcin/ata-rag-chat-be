package com.ata.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ata.rag.ingestion.embedding.EmbeddingService;
import com.ata.rag.repository.ChunkJdbcRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrievalServiceTest {

    @Test
    void lexicalResultWinsAndComputerScienceExpandsToInformatyka() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        ChunkJdbcRepository chunks = mock(ChunkJdbcRepository.class);
        when(embeddings.embed(any())).thenReturn(List.of(new float[] {1, 0, 0}));
        when(chunks.searchByVector(any(), anyInt())).thenReturn(List.of());
        when(chunks.searchLexical(any(), anyInt()))
                .thenAnswer(invocation -> {
                    String query = invocation.getArgument(0);
                    assertTrue(query.contains("informatyka"));
                    return List.of(new ChunkJdbcRepository.SearchChunk(
                            UUID.randomUUID(),
                            "## Informatyka\n- Monthly tuition (10 installments): 1000 PLN",
                            "Tuition > Informatyka",
                            "https://akademiata.pl/kalkulator-czesnego/",
                            "ATA Tuition Calculator",
                            "pricing",
                            0.82));
                });

        RetrievalResult result =
                new RetrievalService(embeddings, chunks)
                        .retrieve("What is tuition for Computer Science?", 5);

        assertEquals(1, result.chunks().size());
        assertEquals("pricing", result.chunks().getFirst().sourceType());
        assertEquals(0.82, result.confidence(), 0.001);
    }
}
