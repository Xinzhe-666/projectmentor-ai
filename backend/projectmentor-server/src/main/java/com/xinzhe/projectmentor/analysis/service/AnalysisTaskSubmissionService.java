package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisTaskSubmissionService {

    private final AnalysisTaskMapper taskMapper;
    private final AnalysisOutboxMapper outboxMapper;
    private final AnalysisOutboxFactory outboxFactory;
    private final AnalysisPipelineProperties properties;

    @Transactional(rollbackFor = Exception.class)
    public void createTaskAndInitialEvent(AnalysisTask task) {
        if (taskMapper.insert(task) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "审计任务创建失败，请稍后重试");
        }
        if (properties.isRabbitMode()) {
            AnalysisOutboxEvent event = outboxFactory.initial(task);
            if (outboxMapper.insert(event) != 1) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "审计任务调度事件创建失败，请稍后重试");
            }
        }
    }
}
