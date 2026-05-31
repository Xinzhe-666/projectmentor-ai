package com.xinzhe.projectmentor.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.admin.vo.AdminMeVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentProjectVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentQaVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentReportVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentUserVO;
import com.xinzhe.projectmentor.admin.vo.AdminStatsVO;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.qa.entity.ProjectQaRecord;
import com.xinzhe.projectmentor.qa.mapper.ProjectQaRecordMapper;
import com.xinzhe.projectmentor.share.entity.ReportShare;
import com.xinzhe.projectmentor.share.mapper.ReportShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int DEFAULT_LIMIT = 10;

    private static final int MAX_LIMIT = 50;

    private final UserMapper userMapper;

    private final ProjectMapper projectMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final ReportShareMapper reportShareMapper;

    private final ProjectQaRecordMapper projectQaRecordMapper;

    @Value("${projectmentor.admin.emails:}")
    private String adminEmails;

    public AdminMeVO getCurrentAdminMe() {
        Long userId = currentUserId();
        User user = userId == null ? null : userMapper.selectById(userId);
        boolean admin = isAdminUser(user);

        return AdminMeVO.builder()
                .admin(admin)
                .userId(admin && user != null ? user.getId() : null)
                .email(admin && user != null ? user.getEmail() : null)
                .build();
    }

    public boolean isCurrentUserAdmin() {
        Long userId = currentUserId();
        if (userId == null) {
            return false;
        }
        return isAdminUser(userMapper.selectById(userId));
    }

    public void requireAdmin() {
        if (!isCurrentUserAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问管理员后台");
        }
    }

    public User requireAdminUser() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (!isAdminUser(user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问管理员后台");
        }
        return user;
    }

    public AdminStatsVO getStats() {
        requireAdmin();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return AdminStatsVO.builder()
                .userCount(userMapper.selectCount(null))
                .projectCount(projectMapper.selectCount(null))
                .reportCount(analysisReportMapper.selectCount(null))
                .qaCount(projectQaRecordMapper.selectCount(notDeletedQaWrapper()))
                .shareCount(reportShareMapper.selectCount(null))
                .todayUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, todayStart)))
                .todayProjectCount(projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                        .ge(Project::getCreateTime, todayStart)))
                .todayReportCount(analysisReportMapper.selectCount(new LambdaQueryWrapper<AnalysisReport>()
                        .ge(AnalysisReport::getCreateTime, todayStart)))
                .todayQaCount(projectQaRecordMapper.selectCount(notDeletedQaWrapper()
                        .ge(ProjectQaRecord::getCreateTime, todayStart)))
                .build();
    }

    public List<AdminRecentUserVO> listRecentUsers(Integer limit) {
        requireAdmin();

        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getEmail, User::getUsername, User::getCreateTime)
                        .orderByDesc(User::getCreateTime)
                        .last(limitClause(limit)))
                .stream()
                .map(user -> AdminRecentUserVO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getUsername())
                        .createTime(user.getCreateTime())
                        .build())
                .toList();
    }

    public List<AdminRecentProjectVO> listRecentProjects(Integer limit) {
        requireAdmin();

        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .select(Project::getId, Project::getUserId, Project::getName,
                                Project::getTechStack, Project::getStatus, Project::getCreateTime)
                        .orderByDesc(Project::getCreateTime)
                        .last(limitClause(limit)))
                .stream()
                .map(project -> AdminRecentProjectVO.builder()
                        .id(project.getId())
                        .userId(project.getUserId())
                        .name(project.getName())
                        .techStack(project.getTechStack())
                        .status(project.getStatus())
                        .createTime(project.getCreateTime())
                        .build())
                .toList();
    }

    public List<AdminRecentReportVO> listRecentReports(Integer limit) {
        requireAdmin();

        List<AnalysisReport> reports = analysisReportMapper.selectList(new LambdaQueryWrapper<AnalysisReport>()
                .select(AnalysisReport::getId, AnalysisReport::getProjectId,
                        AnalysisReport::getTotalScore, AnalysisReport::getCreateTime)
                .orderByDesc(AnalysisReport::getCreateTime)
                .last(limitClause(limit)));

        Map<Long, Project> projectMap = loadProjectMap(reports.stream()
                .map(AnalysisReport::getProjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return reports.stream()
                .map(report -> {
                    Project project = projectMap.get(report.getProjectId());
                    return AdminRecentReportVO.builder()
                            .id(report.getId())
                            .projectId(report.getProjectId())
                            .userId(project == null ? null : project.getUserId())
                            .totalScore(report.getTotalScore())
                            .createTime(report.getCreateTime())
                            .build();
                })
                .toList();
    }

    public List<AdminRecentQaVO> listRecentQa(Integer limit) {
        requireAdmin();

        return projectQaRecordMapper.selectList(notDeletedQaWrapper()
                        .select(ProjectQaRecord::getId, ProjectQaRecord::getUserId,
                                ProjectQaRecord::getProjectId, ProjectQaRecord::getQuestion,
                                ProjectQaRecord::getAiUsed, ProjectQaRecord::getCreateTime)
                        .orderByDesc(ProjectQaRecord::getCreateTime)
                        .last(limitClause(limit)))
                .stream()
                .map(record -> AdminRecentQaVO.builder()
                        .id(record.getId())
                        .userId(record.getUserId())
                        .projectId(record.getProjectId())
                        .question(record.getQuestion())
                        .aiUsed(record.getAiUsed() != null && record.getAiUsed() == 1)
                        .createTime(record.getCreateTime())
                        .build())
                .toList();
    }

    private boolean isAdminUser(User user) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return false;
        }

        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        return parseAdminEmails().contains(email);
    }

    private Set<String> parseAdminEmails() {
        if (!StringUtils.hasText(adminEmails)) {
            return Collections.emptySet();
        }

        Set<String> emails = new LinkedHashSet<>();
        for (String email : adminEmails.split(",")) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                emails.add(normalized);
            }
        }
        return emails;
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private String limitClause(Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : limit;
        safeLimit = Math.max(1, Math.min(safeLimit, MAX_LIMIT));
        return "LIMIT " + safeLimit;
    }

    private LambdaQueryWrapper<ProjectQaRecord> notDeletedQaWrapper() {
        return new LambdaQueryWrapper<ProjectQaRecord>()
                .eq(ProjectQaRecord::getDeleted, 0);
    }

    private Map<Long, Project> loadProjectMap(Set<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return new HashMap<>();
        }

        return projectMapper.selectByIds(projectIds)
                .stream()
                .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));
    }
}
