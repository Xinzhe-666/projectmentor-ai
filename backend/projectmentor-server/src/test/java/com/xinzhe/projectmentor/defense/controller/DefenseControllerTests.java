package com.xinzhe.projectmentor.defense.controller;

import com.xinzhe.projectmentor.defense.dto.DefenseAnswerResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionReviewResponse;
import com.xinzhe.projectmentor.defense.service.DefenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefenseControllerTests {

    private DefenseService defenseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        defenseService = mock(DefenseService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DefenseController(defenseService)).build();
    }

    @Test
    void exposesDefenseMvpEndpoints() throws Exception {
        DefenseSessionResponse session = DefenseSessionResponse.builder()
                .id(100L)
                .projectId(42L)
                .reportId(84L)
                .mode(DefenseService.MODE_EVIDENCE_DEFENSE)
                .status(DefenseService.STATUS_ACTIVE)
                .questionCount(1)
                .build();
        DefenseAnswerResponse answer = DefenseAnswerResponse.builder()
                .id(300L)
                .questionId(200L)
                .answerText("回答")
                .evaluationStatus(DefenseService.ALIGNMENT_PARTIAL)
                .build();
        DefenseSessionReviewResponse review = DefenseSessionReviewResponse.builder()
                .session(session)
                .questions(List.of())
                .answeredCount(1)
                .supportedCount(0)
                .partialCount(1)
                .insufficientCount(0)
                .build();

        when(defenseService.createSession(eq(42L), any())).thenReturn(session);
        when(defenseService.getQuestions(100L)).thenReturn(List.of());
        when(defenseService.submitAnswer(eq(200L), any())).thenReturn(answer);
        when(defenseService.getSessionReview(100L)).thenReturn(review);

        mockMvc.perform(post("/api/projects/42/defense/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":84}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100));

        mockMvc.perform(get("/api/defense/sessions/100/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/api/defense/questions/200/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\":\"回答\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.evaluationStatus").value("PARTIAL"));

        mockMvc.perform(get("/api/defense/sessions/100/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.answeredCount").value(1));
    }
}
