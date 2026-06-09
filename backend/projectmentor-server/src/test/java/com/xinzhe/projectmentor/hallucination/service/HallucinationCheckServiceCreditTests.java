package com.xinzhe.projectmentor.hallucination.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.hallucination.dto.HallucinationCheckRequest;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HallucinationCheckServiceCreditTests {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void doesNotCallAiWhenCreditsAreInsufficient() {
        UserContext.setUserId(7L);
        CreditService creditService = mock(CreditService.class);
        LlmClient llmClient = mock(LlmClient.class);
        doThrow(new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "额度不足，无法调用 AI"))
                .when(creditService)
                .consumeCredits(
                        eq(7L),
                        eq(CreditCostConstants.AI_HALLUCINATION_CHECK),
                        eq(CreditCostConstants.OP_AI_HALLUCINATION_CHECK),
                        eq(null),
                        anyString()
                );

        ObjectMapper objectMapper = new ObjectMapper();
        HallucinationCheckService service = new HallucinationCheckService(
                mock(ProjectMapper.class),
                mock(ProjectFileMapper.class),
                creditService,
                llmClient,
                new HallucinationPromptBuilder(objectMapper),
                new AiJsonUtil(objectMapper)
        );
        HallucinationCheckRequest request = new HallucinationCheckRequest();
        request.setAiAnswer("这个项目已经达到企业级生产标准，可以直接写进简历。");

        assertThatThrownBy(() -> service.check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("额度不足");

        verify(llmClient, never()).chat(anyString(), anyString(), anyString());
        verify(creditService, never()).refundCredits(
                eq(7L),
                eq(CreditCostConstants.AI_HALLUCINATION_CHECK),
                anyString(),
                eq(null),
                anyString()
        );
    }
}
