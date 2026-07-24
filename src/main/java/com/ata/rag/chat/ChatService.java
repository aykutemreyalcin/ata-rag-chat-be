package com.ata.rag.chat;

import com.ata.rag.config.RagProperties;
import com.ata.rag.dto.ChatDoneEvent;
import com.ata.rag.dto.SourceCitation;
import com.ata.rag.generation.GenerationResult;
import com.ata.rag.generation.GenerationService;
import com.ata.rag.model.ChatQueryEntity;
import com.ata.rag.repository.ChatQueryRepository;
import com.ata.rag.retrieval.RetrievalResult;
import com.ata.rag.retrieval.RetrievalService;
import com.ata.rag.retrieval.RetrievedChunk;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final long SSE_TIMEOUT_MS = 120_000;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\S+\\s*");
    private static final String UNKNOWN_EN =
            "I couldn't find this information on the AkademiaTA website.";
    private static final String UNKNOWN_PL =
            "Nie udało mi się znaleźć tej informacji na stronie AkademiaTA.";

    private final RetrievalService retrievalService;
    private final GenerationService generationService;
    private final ChatQueryRepository chatQueryRepository;
    private final RagProperties properties;
    private final ChatMetrics chatMetrics;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatService(
            RetrievalService retrievalService,
            GenerationService generationService,
            ChatQueryRepository chatQueryRepository,
            RagProperties properties,
            ChatMetrics chatMetrics) {
        this.retrievalService = retrievalService;
        this.generationService = generationService;
        this.chatQueryRepository = chatQueryRepository;
        this.properties = properties;
        this.chatMetrics = chatMetrics;
    }

    public SseEmitter stream(String question, int topK) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.submit(() -> run(question, topK, emitter));
        return emitter;
    }

    private void run(String question, int topK, SseEmitter emitter) {
        long startedNanos = System.nanoTime();
        ChatQueryEntity query = new ChatQueryEntity();
        query.setQuestion(question);

        try {
            RetrievalResult retrieval = retrievalService.retrieve(question, topK);
            boolean answered = retrieval.answered(properties.confidenceThreshold());
            List<SourceCitation> sources =
                    answered ? toSources(retrieval.chunks()) : List.of();
            send(emitter, "sources", Map.of("sources", sources));

            GenerationResult generation;
            if (answered) {
                generation = generationService.generate(question, retrieval.chunks());
            } else {
                generation = new GenerationResult(unknownAnswer(question), "confidence-fallback");
            }

            streamTokens(emitter, generation.text());
            long latencyMs = elapsedMs(startedNanos);
            query.setAnswer(generation.text());
            query.setAnswered(answered);
            query.setConfidence(retrieval.confidence());
            query.setRetrievalScore(retrieval.confidence());
            query.setSourceCount(sources.size());
            query.setLatencyMs(latencyMs);
            query.setModel(generation.model());
            chatQueryRepository.save(query);
            chatMetrics.record(answered, retrieval.confidence(), latencyMs, generation);

            send(
                    emitter,
                    "done",
                    new ChatDoneEvent(
                            retrieval.confidence(),
                            answered,
                            sources.size(),
                            latencyMs,
                            generation.model()));

            log.info(
                    "rag.chat.success answered={} confidence={} sourceCount={} latencyMs={} model={}",
                    answered,
                    retrieval.confidence(),
                    sources.size(),
                    latencyMs,
                    generation.model());
            emitter.complete();
        } catch (Exception exception) {
            long latencyMs = elapsedMs(startedNanos);
            query.setAnswered(false);
            query.setLatencyMs(latencyMs);
            query.setErrorMessage(truncate(exception.getMessage()));
            safeSave(query);
            chatMetrics.recordFailure(latencyMs);
            log.error("rag.chat.failed latencyMs={}", latencyMs, exception);
            try {
                send(emitter, "error", Map.of(
                        "message", "The chat request could not be completed.",
                        "code", "CHAT_FAILED"));
                emitter.complete();
            } catch (Exception sendError) {
                emitter.completeWithError(exception);
            }
        }
    }

    private static List<SourceCitation> toSources(List<RetrievedChunk> chunks) {
        Map<String, SourceCitation> byUrl = new LinkedHashMap<>();
        for (RetrievedChunk chunk : chunks) {
            byUrl.putIfAbsent(
                    chunk.url(),
                    new SourceCitation(
                            blankFallback(chunk.title(), "AkademiaTA"),
                            chunk.url(),
                            chunk.section(),
                            chunk.sourceType(),
                            chunk.score()));
        }
        return List.copyOf(byUrl.values());
    }

    private static void streamTokens(SseEmitter emitter, String answer) throws IOException {
        Matcher matcher = TOKEN_PATTERN.matcher(answer);
        while (matcher.find()) {
            send(emitter, "token", Map.of("text", matcher.group()));
        }
    }

    private static void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
    }

    private static String unknownAnswer(String question) {
        String lower = question.toLowerCase();
        boolean polish = lower.matches(".*[ąćęłńóśźż].*")
                || lower.contains(" ile ")
                || lower.startsWith("ile ")
                || lower.contains(" jakie ")
                || lower.startsWith("jak");
        return polish ? UNKNOWN_PL : UNKNOWN_EN;
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 2_000 ? value.substring(0, 2_000) : value;
    }

    private void safeSave(ChatQueryEntity query) {
        try {
            chatQueryRepository.save(query);
        } catch (RuntimeException persistenceError) {
            log.error(
                    "rag.chat.query_log_failed error={}",
                    persistenceError.getMessage());
        }
    }

    @PreDestroy
    void closeExecutor() {
        executor.close();
    }
}
