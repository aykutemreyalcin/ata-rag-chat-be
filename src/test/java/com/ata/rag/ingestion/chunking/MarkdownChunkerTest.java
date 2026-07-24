package com.ata.rag.ingestion.chunking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownChunkerTest {

    @Test
    void chunksByHeadings() {
        String markdown =
                """
                # Admissions
                ## Required documents
                Passport, diploma and application form are required for admissions.
                ## Deadlines
                The admission period begins in May and ends in September.
                # Tuition
                ## Computer Science
                Monthly tuition depends on installment plan.
                """;
        List<TextChunk> chunks = MarkdownChunker.chunkMarkdown(markdown);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.section().contains("Admissions")));
    }

    @Test
    void emptyMarkdownYieldsNoChunks() {
        assertTrue(MarkdownChunker.chunkMarkdown("").isEmpty());
    }
}
