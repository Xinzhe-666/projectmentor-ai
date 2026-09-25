package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisReportPersistenceService {

    private final AnalysisReportMapper analysisReportMapper;

    private final ProjectMapper projectMapper;

    private final AnalysisTaskMapper analysisTaskMapper;

    @Transactional(rollbackFor = Exception.class)
    public void saveReportAndMarkProjectFinished(AnalysisReport report, Project project) {
        try {
            if (analysisReportMapper.insert(report) != 1) {
                throw new BusinessException(
                        ErrorCode.OPERATION_ERROR,
                        "AI 审计报告已生成但保存失败，额度已返还，请稍后重试。"
                );
            }

            project.setStatus("FINISHED");
            if (projectMapper.updateById(project) != 1) {
                throw new BusinessException(
                        ErrorCode.OPERATION_ERROR,
                        "AI 审计报告状态保存失败，额度已返还，请稍后重试。"
                );
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "AI 审计报告保存失败，额度已返还，请稍后重试。"
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveReportAndCompleteTask(AnalysisReport report,
                                          Project project,
                                          AnalysisExecutionContext execution) {
        if (analysisTaskMapper.selectOwnedExecutionForUpdate(
                execution.taskId(), execution.workerId(), execution.executionVersion()
        ) == null) {
            throw new StaleAnalysisExecutionException();
        }

        report.setTaskId(execution.taskId());
        if (analysisReportMapper.insert(report) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "分析报告保存失败");
        }

        project.setStatus("FINISHED");
        if (projectMapper.updateById(project) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目分析状态保存失败");
        }

        if (analysisTaskMapper.completeSuccess(
                execution.taskId(), execution.workerId(), execution.executionVersion(), report.getId()
        ) != 1) {
            throw new StaleAnalysisExecutionException();
        }
    }
}
