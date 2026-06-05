package com.xinzhe.projectmentor.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisReportService;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.dashboard.vo.DashboardSummaryVO;
import com.xinzhe.projectmentor.interview.service.InterviewService;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.project.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_LIMIT = 5;

    private final ProjectMapper projectMapper;

    private final CreditService creditService;

    private final AnalysisReportService analysisReportService;

    private final InterviewService interviewService;

    public DashboardSummaryVO getSummary() {
        Long userId = getCurrentUserId();
        Long projectCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId));

        return DashboardSummaryVO.builder()
                .projectCount(projectCount == null ? 0L : projectCount)
                .creditBalance(creditService.getMyCredits().getRemainingCredits())
                .reportCount(analysisReportService.countMyReports())
                .interviewSessionCount(interviewService.countMySessions())
                .recentProjects(listRecentProjects(userId))
                .recentReports(analysisReportService.listRecentMyReports(RECENT_LIMIT))
                .recentInterviews(interviewService.listRecentMySessions(RECENT_LIMIT))
                .build();
    }

    private List<ProjectVO> listRecentProjects(Long userId) {
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .select(Project::getId, Project::getName, Project::getTechStack,
                                Project::getStatus, Project::getCreateTime, Project::getUpdateTime)
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreateTime)
                        .last("LIMIT " + RECENT_LIMIT))
                .stream()
                .map(this::toProjectVO)
                .toList();
    }

    private ProjectVO toProjectVO(Project project) {
        return ProjectVO.builder()
                .id(project.getId())
                .name(project.getName())
                .techStack(project.getTechStack())
                .status(project.getStatus())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }
}
