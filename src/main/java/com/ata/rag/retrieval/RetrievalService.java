package com.ata.rag.retrieval;

import com.ata.rag.ingestion.embedding.EmbeddingService;
import com.ata.rag.repository.ChunkJdbcRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {
    private final EmbeddingService embeddingService;
    private final ChunkJdbcRepository chunkRepository;

    public RetrievalService(
            EmbeddingService embeddingService,
            ChunkJdbcRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    public RetrievalResult retrieve(String question, int topK) {
        String lexicalQuery = expandForLexicalSearch(question);
        List<ChunkJdbcRepository.SearchChunk> vector = List.of();
        if (embeddingService.isSemantic()) {
            float[] queryEmbedding = embeddingService.embed(List.of(question)).getFirst();
            vector = chunkRepository.searchByVector(queryEmbedding, Math.max(topK * 3, 10));
        }
        List<ChunkJdbcRepository.SearchChunk> lexical =
                lexicalQuery.isBlank()
                        ? List.of()
                        : chunkRepository.searchLexical(lexicalQuery, Math.max(topK * 3, 15));

        Map<UUID, RetrievedChunk> merged = new LinkedHashMap<>();
        for (ChunkJdbcRepository.SearchChunk chunk : lexical) {
            double lexicalScore = lexicalScore(lexicalQuery, chunk);
            if (lexicalScore >= 0.55) {
                merged.put(chunk.id(), map(chunk, lexicalScore));
            }
        }
        for (ChunkJdbcRepository.SearchChunk chunk : vector) {
            RetrievedChunk candidate = map(chunk, chunk.score());
            merged.merge(chunk.id(), candidate, (left, right) ->
                    left.score() >= right.score() ? left : right);
        }

        List<RetrievedChunk> ranked = new ArrayList<>(merged.values());
        ranked.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        if (ranked.size() > topK) {
            ranked = new ArrayList<>(ranked.subList(0, topK));
        }
        double confidence = ranked.isEmpty() ? 0 : ranked.getFirst().score();
        return new RetrievalResult(List.copyOf(ranked), confidence);
    }

    static String expandForLexicalSearch(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        List<String> terms = new ArrayList<>();
        for (String token : normalized
                .replaceAll("[^\\p{L}\\p{N}\\s-]", " ")
                .split("\\s+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                terms.add(token);
            }
        }
        if (normalized.contains("computer science")) {
            terms.add("informatyka");
        }
        if (normalized.contains("warsaw")) {
            terms.add("warszawa");
        }
        if (normalized.contains("wroclaw") || normalized.contains("wrocław")) {
            terms.add("wrocław");
        }
        if (normalized.contains("tuition") || normalized.contains("fee")) {
            terms.add("tuition");
            terms.add("czesne");
        }
        if (normalized.contains("how much") || normalized.contains("cost")) {
            terms.add("r10");
            terms.add("r12");
        }
        return String.join(" OR ", terms.stream().distinct().toList());
    }

    private static double lexicalScore(
            String lexicalQuery, ChunkJdbcRepository.SearchChunk chunk) {
        Set<String> terms = List.of(lexicalQuery.split("\\s+OR\\s+")).stream()
                .map(String::strip)
                .filter(term -> !term.isBlank())
                .collect(Collectors.toSet());
        String haystack = ((chunk.title() == null ? "" : chunk.title())
                        + " "
                        + (chunk.section() == null ? "" : chunk.section())
                        + " "
                        + chunk.content())
                .toLowerCase(Locale.ROOT);
        long hits = terms.stream().filter(haystack::contains).count();
        int requiredHits = terms.size() <= 2 ? 1 : 2;
        if (hits < requiredHits) {
            return 0;
        }
        return Math.min(0.98, 0.62 + hits * 0.10);
    }

    private static RetrievedChunk map(
            ChunkJdbcRepository.SearchChunk chunk, double score) {
        return new RetrievedChunk(
                chunk.id(),
                chunk.content(),
                chunk.section(),
                chunk.url(),
                chunk.title(),
                chunk.sourceType(),
                score);
    }

    private static final List<String> STOP_WORDS = List.of(
            "what", "which", "where", "when", "with", "from", "that", "this",
            "the", "and", "for", "are", "how", "much", "does", "is",
            "university", "website", "akademiata",
            "ile", "jest", "dla", "oraz", "czy", "jakie", "jaki", "jaka");
}
