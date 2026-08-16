package com.xinzhe.projectmentor.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.interview.entity.InterviewMessage;
import com.xinzhe.projectmentor.interview.entity.InterviewSession;
import com.xinzhe.projectmentor.interview.mapper.InterviewMessageMapper;
import com.xinzhe.projectmentor.interview.mapper.InterviewSessionMapper;
import com.xinzhe.projectmentor.project.dto.CreateProjectRequest;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.project.vo.ProjectVO;
import com.xinzhe.projectmentor.qa.mapper.ProjectQaRecordMapper;
import com.xinzhe.projectmentor.share.entity.ReportShare;
import com.xinzhe.projectmentor.share.mapper.ReportShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectQaRecordMapper projectQaRecordMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final ReportShareMapper reportShareMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewMessageMapper interviewMessageMapper;

    public ProjectVO createProject(CreateProjectRequest request) {
        Long userId = getCurrentUserId();

        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.getName());
        project.setGithubUrl(request.getGithubUrl());
        project.setDescription(request.getDescription());
        project.setProjectType(request.getProjectType());
        project.setTechStack(request.getTechStack());
        project.setStatus("PENDING");

        projectMapper.insert(project);

        return toVO(project);
    }

    public List<ProjectVO> listMyProjects() {
        Long userId = getCurrentUserId();

        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreateTime)
        );

        return projects.stream()
                .map(this::toVO)
                .toList();
    }

    public ProjectVO getProjectDetail(Long projectId) {
        Long userId = getCurrentUserId();

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限访问");
        }

        return toVO(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        Long userId = getCurrentUserId();

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限删除");
        }

        Long activeTaskCount = analysisTaskMapper.selectCount(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getProjectId, projectId)
                        .in(AnalysisTask::getStatus, List.of("PENDING", "RUNNING"))
        );
        if (activeTaskCount != null && activeTaskCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目正在分析中，请等待分析完成后再删除");
        }

        List<Long> reportIds = listReportIds(projectId);
        if (!reportIds.isEmpty()) {
            reportShareMapper.delete(
                    new LambdaQueryWrapper<ReportShare>()
                            .in(ReportShare::getReportId, reportIds)
            );
        }

        List<Long> sessionIds = listSessionIds(projectId);
        if (!sessionIds.isEmpty()) {
            interviewMessageMapper.delete(
                    new LambdaQueryWrapper<InterviewMessage>()
                            .in(InterviewMessage::getSessionId, sessionIds)
            );
        }

        // QA history is physically removed here, including rows hidden by its deleted flag.
        projectQaRecordMapper.physicalDeleteByProjectId(projectId);
        analysisTaskMapper.delete(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getProjectId, projectId)
        );
        analysisReportMapper.delete(
                new LambdaQueryWrapper<AnalysisReport>()
                        .eq(AnalysisReport::getProjectId, projectId)
        );
        interviewSessionMapper.delete(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getProjectId, projectId)
        );
        projectFileMapper.delete(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
        );

        // Credit and AI call logs are audit records; feedback and account data also remain intact.
        if (projectMapper.deleteById(projectId) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目删除失败，请稍后重试");
        }
    }

    private List<Long> listReportIds(Long projectId) {
        List<AnalysisReport> reports = analysisReportMapper.selectList(
                new LambdaQueryWrapper<AnalysisReport>()
                        .select(AnalysisReport::getId)
                        .eq(AnalysisReport::getProjectId, projectId)
        );
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        return reports.stream()
                .filter(Objects::nonNull)
                .map(AnalysisReport::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> listSessionIds(Long projectId) {
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .select(InterviewSession::getId)
                        .eq(InterviewSession::getProjectId, projectId)
        );
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        return sessions.stream()
                .filter(Objects::nonNull)
                .map(InterviewSession::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private ProjectVO toVO(Project project) {
        return ProjectVO.builder()
                .id(project.getId())
                .name(project.getName())
                .githubUrl(project.getGithubUrl())
                .description(project.getDescription())
                .projectType(project.getProjectType())
                .techStack(project.getTechStack())
                .status(project.getStatus())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }
}
