package com.ata.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
        when(embeddings.isSemantic()).thenReturn(false);
        ChunkJdbcRepository.SearchChunk pricingChunk = new ChunkJdbcRepository.SearchChunk(
                UUID.randomUUID(),
                "## Informatyka\n- Monthly tuition (10 installments): 1000 PLN",
                "Tuition > Informatyka",
                "https://akademiata.pl/kalkulator-czesnego/",
                "ATA Tuition Calculator",
                "pricing",
                0.82);
        when(chunks.searchLexical(any(), anyInt()))
                .thenAnswer(invocation -> {
                    String query = invocation.getArgument(0);
                    assertTrue(query.contains("informatyka"));
                    assertTrue(query.contains("|"));
                    return List.of(pricingChunk);
                });
        when(chunks.searchLexicalBySourceType(any(), eq("pricing"), anyInt()))
                .thenReturn(List.of(pricingChunk));

        RetrievalResult result =
                new RetrievalService(embeddings, chunks)
                        .retrieve("What is tuition for Computer Science?", 5);

        assertEquals(1, result.chunks().size());
        assertEquals("pricing", result.chunks().getFirst().sourceType());
        assertTrue(result.confidence() >= 0.82);
    }

    @Test
    void prefersPricingChunksForTuitionQuestions() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        ChunkJdbcRepository chunks = mock(ChunkJdbcRepository.class);
        when(embeddings.isSemantic()).thenReturn(false);
        when(chunks.searchLexical(any(), anyInt())).thenReturn(List.of(
                new ChunkJdbcRepository.SearchChunk(
                        UUID.randomUUID(),
                        "Computer networks and cybersecurity coming soon",
                        "Offer",
                        "https://akademiata.pl/en/",
                        "Offer",
                        "html",
                        0.9)));
        when(chunks.searchLexicalBySourceType(any(), eq("pricing"), anyInt()))
                .thenReturn(List.of(new ChunkJdbcRepository.SearchChunk(
                        UUID.randomUUID(),
                        "Tuition — Computer networks and cybersecurity\nCity: Wrocław\nEU / CIS / Ukraine — annual tuition: 2600 EUR",
                        "Tuition",
                        "https://akademiata.pl/en/offer/bachelor/wroclaw-computer-networks-and-cybersecurity/",
                        "Tuition — Computer networks and cybersecurity",
                        "pricing",
                        0.8)));

        RetrievalResult result = new RetrievalService(embeddings, chunks)
                .retrieve(
                        "What is the annual tuition for Computer networks and cybersecurity in Wrocław?",
                        5);

        assertEquals("pricing", result.chunks().getFirst().sourceType());
        assertTrue(result.chunks().getFirst().content().contains("2600"));
    }
}
