package com.ata.rag.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ata.rag.chat.ChatService;
import com.ata.rag.ingestion.pipeline.SyncJobService;
import com.ata.rag.repository.ChatAnalyticsRepository;
import com.ata.rag.repository.ChunkJdbcRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChunkJdbcRepository chunkJdbcRepository;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatAnalyticsRepository chatAnalyticsRepository;

    @MockitoBean
    private SyncJobService syncJobService;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("ata-rag-chat-be"));
    }

    @Test
    void chatStartsSseStream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(chatService.stream("What is tuition for Computer Science?", 5)).thenReturn(emitter);
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"question\":\"What is tuition for Computer Science?\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void adminSummaryIsAvailable() throws Exception {
        when(chunkJdbcRepository.countAll()).thenReturn(0L);
        when(chunkJdbcRepository.countBySourceType()).thenReturn(Map.of());
        when(chatAnalyticsRepository.summary())
                .thenReturn(new ChatAnalyticsRepository.ChatSummary(3, 2, 1, 0.75, 120.0));
        mockMvc.perform(get("/api/admin/summary").with(httpBasic("admin", "test-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page_count").exists())
                .andExpect(jsonPath("$.chunk_count").value(0))
                .andExpect(jsonPath("$.total_questions").value(3))
                .andExpect(jsonPath("$.avg_confidence").value(0.75));
    }

    @Test
    void adminRequiresBasicAuth() throws Exception {
        mockMvc.perform(get("/api/admin/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/summary").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminQuestionsReturnsTopAndUnanswered() throws Exception {
        when(chatAnalyticsRepository.topQuestions(10))
                .thenReturn(List.of(new ChatAnalyticsRepository.TopQuestion("Tuition?", 2)));
        when(chatAnalyticsRepository.unanswered(10))
                .thenReturn(List.of(new ChatAnalyticsRepository.UnansweredQuestion(
                        "Moon observatory?", Instant.parse("2026-07-24T12:00:00Z"))));

        mockMvc.perform(get("/api/admin/questions").with(httpBasic("admin", "test-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.top_questions[0].question").value("Tuition?"))
                .andExpect(jsonPath("$.top_questions[0].count").value(2))
                .andExpect(jsonPath("$.unanswered[0].question").value("Moon observatory?"));
    }

    @Test
    void prometheusEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }
}
