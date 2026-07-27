package com.ata.rag.ingestion.pipeline;

import com.ata.rag.ingestion.chunking.MarkdownChunker;
import com.ata.rag.ingestion.chunking.TextChunk;
import com.ata.rag.ingestion.content.ContentHashes;
import com.ata.rag.ingestion.embedding.EmbeddingService;
import com.ata.rag.model.PageEntity;
import com.ata.rag.repository.ChunkJdbcRepository;
import com.ata.rag.repository.PageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PageIngestService {
    private final PageRepository pageRepository;
    private final ChunkJdbcRepository chunkJdbcRepository;
    private final EmbeddingService embeddingService;

    public PageIngestService(
            PageRepository pageRepository,
            ChunkJdbcRepository chunkJdbcRepository,
            EmbeddingService embeddingService) {
        this.pageRepository = pageRepository;
        this.chunkJdbcRepository = chunkJdbcRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public IngestResult ingest(ProcessedPage processed) {
        Instant now = Instant.now();
        PageEntity page = pageRepository.findByUrl(processed.url()).orElse(null);

        if (page != null && processed.contentHash().equals(page.getContentHash()) && "active".equals(page.getStatus())) {
            page.setLastCrawledAt(now);
            page.setHttpStatus(processed.httpStatus());
            page.setStatus("active");
            page.setConsecutiveMissCount(0);
            page.setErrorMessage(null);
            pageRepository.save(page);
            return new IngestResult(page.getId(), true, 0, 0, 0);
        }

        if (page == null) {
            page = new PageEntity();
            page.setUrl(processed.url());
        } else if ("pricing".equals(page.getSourceType())
                && !"pricing".equals(processed.sourceType())) {
            // Programme pages are also used as pricing document URLs. Never let the
            // website crawl replace official calculator tuition rows with HTML stubs.
            page.setLastCrawledAt(now);
            page.setConsecutiveMissCount(0);
            pageRepository.save(page);
            return new IngestResult(page.getId(), true, 0, 0, 0);
        }
        page.setTitle(processed.title());
        page.setSourceType(processed.sourceType());
        page.setLanguage(processed.language());
        page.setContentHash(processed.contentHash());
        page.setLastCrawledAt(now);
        page.setHttpStatus(processed.httpStatus());
        page.setStatus("active");
        page.setConsecutiveMissCount(0);
        page.setErrorMessage(null);
        page = pageRepository.saveAndFlush(page);

        List<TextChunk> chunks = MarkdownChunker.chunkMarkdown(processed.markdown());
        Map<String, IndexedChunk> newByHash = new HashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            newByHash.put(ContentHashes.sha256(chunk.text()), new IndexedChunk(i, chunk));
        }

        List<ChunkJdbcRepository.ExistingChunk> existing = chunkJdbcRepository.findByPageId(page.getId());
        Map<String, ChunkJdbcRepository.ExistingChunk> existingByHash = new HashMap<>();
        for (ChunkJdbcRepository.ExistingChunk chunk : existing) {
            existingByHash.put(chunk.contentHash(), chunk);
        }

        List<String> hashesToEmbed = new ArrayList<>();
        List<String> textsToEmbed = new ArrayList<>();
        for (Map.Entry<String, IndexedChunk> entry : newByHash.entrySet()) {
            if (!existingByHash.containsKey(entry.getKey())) {
                hashesToEmbed.add(entry.getKey());
                textsToEmbed.add(entry.getValue().chunk().text());
            }
        }

        List<float[]> embeddings =
                textsToEmbed.isEmpty() ? List.of() : embeddingService.embed(textsToEmbed);
        Map<String, float[]> embeddingsByHash = new HashMap<>();
        for (int i = 0; i < hashesToEmbed.size(); i++) {
            embeddingsByHash.put(hashesToEmbed.get(i), embeddings.get(i));
        }

        int created = 0;
        int reused = 0;
        for (Map.Entry<String, IndexedChunk> entry : newByHash.entrySet()) {
            String hash = entry.getKey();
            IndexedChunk indexed = entry.getValue();
            ChunkJdbcRepository.ExistingChunk existingChunk = existingByHash.get(hash);
            if (existingChunk != null) {
                chunkJdbcRepository.updateMetadata(
                        existingChunk.id(),
                        indexed.index(),
                        indexed.chunk().section(),
                        indexed.chunk().tokenCount(),
                        processed.language(),
                        processed.title());
                reused++;
            } else {
                chunkJdbcRepository.insert(
                        page.getId(),
                        page.getId().toString(),
                        indexed.index(),
                        indexed.chunk().text(),
                        hash,
                        indexed.chunk().section(),
                        indexed.chunk().tokenCount(),
                        processed.language(),
                        processed.url(),
                        processed.title(),
                        processed.sourceType(),
                        page.getLastModified(),
                        embeddingsByHash.get(hash),
                        embeddingService.model());
                created++;
            }
        }

        Set<String> stale = new HashSet<>(existingByHash.keySet());
        stale.removeAll(newByHash.keySet());
        List<UUID> staleIds = stale.stream().map(hash -> existingByHash.get(hash).id()).toList();
        chunkJdbcRepository.deleteByIds(staleIds);

        return new IngestResult(page.getId(), false, created, reused, staleIds.size());
    }

    public record IngestResult(UUID pageId, boolean skipped, int created, int reused, int deleted) {}

    private record IndexedChunk(int index, TextChunk chunk) {}
}
