package com.xinzhe.projectmentor.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiProperties;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.qa.entity.ProjectQaRecord;
import com.xinzhe.projectmentor.qa.mapper.ProjectQaRecordMapper;
import com.xinzhe.projectmentor.qa.vo.ProjectQaResponseVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectQaServiceCreditTests {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void refundsCreditsWhenAiAnswerFails() {
        TestFixture fixture = createFixture();
        when(fixture.llmClient.chat(eq("PROJECT_QA"), anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务不可用"));

        ProjectQaResponseVO response = fixture.service.ask(100L, "JWT token 在哪里生成和校验？");

        assertThat(response.getAiUsed()).isFalse();
        assertThat(response.getAnswer()).contains("额度已返还");
        verify(fixture.creditService).consumeCredits(
                7L,
                CreditCostConstants.AI_PROJECT_QA,
                CreditCostConstants.OP_AI_PROJECT_QA,
                100L,
                "AI 项目问答"
        );
        verify(fixture.creditService).refundCredits(
                7L,
                CreditCostConstants.AI_PROJECT_QA,
                CreditCostConstants.OP_AI_PROJECT_QA_REFUND,
                100L,
                "AI 项目问答失败返还"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void listingHistoryDoesNotConsumeCreditsOrCallAi() {
        TestFixture fixture = createFixture();
        when(fixture.recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(fixture.service.listHistory(100L)).isEmpty();

        verify(fixture.creditService, never()).consumeCredits(any(), anyInt(), anyString(), any(), anyString());
        verify(fixture.creditService, never()).refundCredits(any(), anyInt(), anyString(), any(), anyString());
        verify(fixture.llmClient, never()).chat(anyString(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    private TestFixture createFixture() {
        UserContext.setUserId(7L);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectFileMapper projectFileMapper = mock(ProjectFileMapper.class);
        ProjectQaRecordMapper recordMapper = mock(ProjectQaRecordMapper.class);
        CreditService creditService = mock(CreditService.class);
        LlmClient llmClient = mock(LlmClient.class);

        Project project = new Project();
        project.setId(100L);
        project.setUserId(7L);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);

        ProjectFile file = new ProjectFile();
        file.setProjectId(100L);
        file.setFilePath("src/main/java/example/AuthService.java");
        file.setFileType("SERVICE");
        file.setContent("JWT token authorization login generateToken validateToken interceptor");
        when(projectFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
        when(recordMapper.insert(any(ProjectQaRecord.class))).thenReturn(1);

        ProjectQaService service = new ProjectQaService(
                projectMapper,
                projectFileMapper,
                recordMapper,
                creditService,
                llmClient,
                new AiProperties(),
                new ObjectMapper()
        );
        return new TestFixture(service, recordMapper, creditService, llmClient);
    }

    private record TestFixture(ProjectQaService service,
                               ProjectQaRecordMapper recordMapper,
                               CreditService creditService,
                               LlmClient llmClient) {
    }
}
