package com.ata.rag.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatAnalyticsRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChatAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ChatSummary summary() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) AS total_questions,
                       COUNT(*) FILTER (WHERE answered) AS answered_questions,
                       COUNT(*) FILTER (WHERE NOT answered) AS unanswered_questions,
                       AVG(confidence) FILTER (WHERE confidence IS NOT NULL) AS avg_confidence,
                       AVG(latency_ms) FILTER (WHERE latency_ms IS NOT NULL) AS avg_latency_ms,
                       COUNT(*) FILTER (WHERE helpful IS TRUE) AS helpful_count,
                       COUNT(*) FILTER (WHERE helpful IS FALSE) AS not_helpful_count,
                       COUNT(*) FILTER (WHERE helpful IS NOT NULL) AS rated_questions
                FROM chat_queries
                """,
                (rs, rowNum) -> new ChatSummary(
                        rs.getLong("total_questions"),
                        rs.getLong("answered_questions"),
                        rs.getLong("unanswered_questions"),
                        nullableDouble(rs, "avg_confidence"),
                        nullableDouble(rs, "avg_latency_ms"),
                        rs.getLong("helpful_count"),
                        rs.getLong("not_helpful_count"),
                        rs.getLong("rated_questions")));
    }

    public List<TopQuestion> topQuestions(int limit) {
        return jdbcTemplate.query(
                """
                SELECT MIN(question) AS question, COUNT(*) AS question_count
                FROM chat_queries
                GROUP BY lower(trim(question))
                ORDER BY question_count DESC, MAX(created_at) DESC
                LIMIT ?
                """,
                (rs, rowNum) ->
                        new TopQuestion(rs.getString("question"), rs.getLong("question_count")),
                limit);
    }

    public List<UnansweredQuestion> unanswered(int limit) {
        return jdbcTemplate.query(
                """
                SELECT question, created_at
                FROM chat_queries
                WHERE answered = FALSE
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new UnansweredQuestion(
                        rs.getString("question"),
                        rs.getTimestamp("created_at").toInstant()),
                limit);
    }

    public List<FeedbackItem> recentFeedback(int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, question, answer, helpful, created_at, feedback_at
                FROM chat_queries
                WHERE helpful IS NOT NULL
                ORDER BY feedback_at DESC NULLS LAST, created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new FeedbackItem(
                        (java.util.UUID) rs.getObject("id"),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getObject("helpful") == null ? null : rs.getBoolean("helpful"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("feedback_at") == null
                                ? null
                                : rs.getTimestamp("feedback_at").toInstant()),
                limit);
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    public record ChatSummary(
            long totalQuestions,
            long answeredQuestions,
            long unansweredQuestions,
            Double averageConfidence,
            Double averageLatencyMs,
            long helpfulCount,
            long notHelpfulCount,
            long ratedQuestions) {
        public Double feedbackRate() {
            if (totalQuestions <= 0) {
                return null;
            }
            return (double) ratedQuestions / (double) totalQuestions;
        }
    }

    public record TopQuestion(String question, long count) {}

    public record UnansweredQuestion(String question, Instant createdAt) {}

    public record FeedbackItem(
            java.util.UUID id,
            String question,
            String answer,
            Boolean helpful,
            Instant createdAt,
            Instant feedbackAt) {}
}
