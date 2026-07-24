package com.ata.rag.repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ChunkJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChunkJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ExistingChunk> findByPageId(UUID pageId) {
        return jdbcTemplate.query(
                """
                SELECT id, content_hash, chunk_index, section, token_count, language, title
                FROM chunks
                WHERE page_id = ?
                """,
                chunkMapper(),
                pageId);
    }

    public void insert(
            UUID pageId,
            String documentId,
            int chunkIndex,
            String content,
            String contentHash,
            String section,
            int tokenCount,
            String language,
            String url,
            String title,
            String sourceType,
            Instant lastModified,
            float[] embedding,
            String embeddingModel) {
        jdbcTemplate.update(
                """
                INSERT INTO chunks (
                    id, page_id, document_id, chunk_index, content, content_hash, section, token_count,
                    language, url, title, source_type, last_modified, embedding, embedding_model
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?)
                """,
                UUID.randomUUID(),
                pageId,
                documentId,
                chunkIndex,
                content,
                contentHash,
                section,
                tokenCount,
                language,
                url,
                title,
                sourceType,
                lastModified == null ? null : Timestamp.from(lastModified),
                toVectorLiteral(embedding),
                embeddingModel);
    }

    public void updateMetadata(
            UUID chunkId, int chunkIndex, String section, int tokenCount, String language, String title) {
        jdbcTemplate.update(
                """
                UPDATE chunks
                SET chunk_index = ?, section = ?, token_count = ?, language = ?, title = ?, updated_at = NOW()
                WHERE id = ?
                """,
                chunkIndex,
                section,
                tokenCount,
                language,
                title,
                chunkId);
    }

    public void deleteByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "DELETE FROM chunks WHERE id = ?",
                ids,
                ids.size(),
                (PreparedStatement ps, UUID id) -> ps.setObject(1, id));
    }

    public void deleteByPageId(UUID pageId) {
        jdbcTemplate.update("DELETE FROM chunks WHERE page_id = ?", pageId);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chunks", Long.class);
        return count == null ? 0 : count;
    }

    public Map<String, Object> countBySourceType() {
        return jdbcTemplate.query(
                "SELECT source_type, COUNT(*) AS count FROM chunks GROUP BY source_type",
                rs -> {
                    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getString("source_type"), rs.getLong("count"));
                    }
                    return map;
                });
    }

    private static RowMapper<ExistingChunk> chunkMapper() {
        return (rs, rowNum) -> new ExistingChunk(
                (UUID) rs.getObject("id"),
                rs.getString("content_hash"),
                rs.getInt("chunk_index"),
                rs.getString("section"),
                rs.getInt("token_count"),
                rs.getString("language"),
                rs.getString("title"));
    }

    public static String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder(embedding.length * 8);
        builder.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        builder.append(']');
        return builder.toString();
    }

    public record ExistingChunk(
            UUID id,
            String contentHash,
            int chunkIndex,
            String section,
            int tokenCount,
            String language,
            String title) {}
}
