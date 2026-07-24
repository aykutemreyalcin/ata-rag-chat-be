package com.ata.rag.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ata.rag.chat.ChatService;
import com.ata.rag.repository.ChunkJdbcRepository;
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
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page_count").exists())
                .andExpect(jsonPath("$.chunk_count").value(0));
    }

    @Test
    void adminQuestionsStillPending() throws Exception {
        mockMvc.perform(get("/api/admin/questions")).andExpect(status().isNotImplemented());
    }
}
