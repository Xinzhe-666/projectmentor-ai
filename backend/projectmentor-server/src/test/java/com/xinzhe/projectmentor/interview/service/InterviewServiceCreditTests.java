package com.xinzhe.projectmentor.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.interview.dto.StartInterviewRequest;
import com.xinzhe.projectmentor.interview.entity.InterviewSession;
import com.xinzhe.projectmentor.interview.mapper.InterviewMessageMapper;
import com.xinzhe.projectmentor.interview.mapper.InterviewSessionMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceCreditTests {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsInterviewBeforeCallingAiWhenCreditsAreInsufficient() {
        UserContext.setUserId(7L);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        CreditService creditService = mock(CreditService.class);
        LlmClient llmClient = mock(LlmClient.class);
        InterviewSessionMapper interviewSessionMapper = mock(InterviewSessionMapper.class);

        Project project = new Project();
        project.setId(100L);
        project.setUserId(7L);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);
        when(interviewSessionMapper.insert(any(InterviewSession.class))).thenReturn(1);
        doThrow(new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "额度不足，无法调用 AI"))
                .when(creditService)
                .consumeCredits(
                        eq(7L),
                        eq(CreditCostConstants.AI_INTERVIEW_SESSION),
                        eq(CreditCostConstants.OP_AI_INTERVIEW_SESSION),
                        eq(null),
                        anyString()
                );

        ObjectMapper objectMapper = new ObjectMapper();
        InterviewService service = new InterviewService(
                interviewSessionMapper,
                mock(InterviewMessageMapper.class),
                projectMapper,
                mock(ProjectFileMapper.class),
                creditService,
                llmClient,
                new InterviewPromptBuilder(),
                new AiJsonUtil(objectMapper)
        );
        StartInterviewRequest request = new StartInterviewRequest();
        request.setProjectId(100L);
        request.setMode("TECH_DEEP_DIVE");

        assertThatThrownBy(() -> service.startInterview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("额度不足");

        verify(llmClient, never()).chat(anyString(), anyString(), anyString());
        verify(creditService, never()).refundCredits(any(), anyInt(), anyString(), any(), anyString());
    }
}
