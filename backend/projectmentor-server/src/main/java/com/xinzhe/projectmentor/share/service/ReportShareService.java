package com.xinzhe.projectmentor.share.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.share.entity.ReportShare;
import com.xinzhe.projectmentor.share.mapper.ReportShareMapper;
import com.xinzhe.projectmentor.share.vo.PublicReportVO;
import com.xinzhe.projectmentor.share.vo.ReportShareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ReportShareService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ReportShareMapper reportShareMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final ProjectMapper projectMapper;

    private final ClaimEvidenceAuditService claimEvidenceAuditService;

    public ReportShareVO getShareInfo(Long reportId) {
        getOwnedReport(reportId);

        ReportShare share = findByReportId(reportId);

        return toShareVO(reportId, share);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportShareVO createOrRefreshShare(Long reportId) {
        getOwnedReport(reportId);
        Long userId = requireUserId();
        String shareToken = generateUniqueShareToken();

        ReportShare share = findByReportId(reportId);

        if (share == null) {
            share = new ReportShare();
            share.setUserId(userId);
            share.setReportId(reportId);
            share.setShareToken(shareToken);
            share.setEnabled(1);
            reportShareMapper.insert(share);
        } else {
            reportShareMapper.update(null,
                    new LambdaUpdateWrapper<ReportShare>()
                            .eq(ReportShare::getId, share.getId())
                            .set(ReportShare::getUserId, userId)
                            .set(ReportShare::getShareToken, shareToken)
                            .set(ReportShare::getEnabled, 1)
                            .set(ReportShare::getExpireTime, null)
            );

            share.setUserId(userId);
            share.setShareToken(shareToken);
            share.setEnabled(1);
            share.setExpireTime(null);
        }

        return toShareVO(reportId, share);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableShare(Long reportId) {
        getOwnedReport(reportId);

        ReportShare share = findByReportId(reportId);

        if (share == null) {
            return;
        }

        reportShareMapper.update(null,
                new LambdaUpdateWrapper<ReportShare>()
                        .eq(ReportShare::getId, share.getId())
                        .set(ReportShare::getEnabled, 0)
        );
    }

    public PublicReportVO getPublicReport(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分享 Token 不能为空");
        }

        ReportShare share = reportShareMapper.selectOne(
                new LambdaQueryWrapper<ReportShare>()
                        .eq(ReportShare::getShareToken, token.trim())
                        .last("LIMIT 1")
        );

        if (share == null || !Integer.valueOf(1).equals(share.getEnabled())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享链接不存在或已关闭");
        }

        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享链接已过期");
        }

        AnalysisReport report = analysisReportMapper.selectById(share.getReportId());

        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在");
        }

        Project project = projectMapper.selectById(report.getProjectId());

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        return toPublicReportVO(project, report);
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private AnalysisReport getOwnedReport(Long reportId) {
        Long userId = requireUserId();
        AnalysisReport report = analysisReportMapper.selectById(reportId);

        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在");
        }

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, report.getProjectId())
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在或无权访问");
        }

        return report;
    }

    private ReportShare findByReportId(Long reportId) {
        return reportShareMapper.selectOne(
                new LambdaQueryWrapper<ReportShare>()
                        .eq(ReportShare::getReportId, reportId)
                        .last("LIMIT 1")
        );
    }

    private String generateUniqueShareToken() {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            Long count = reportShareMapper.selectCount(
                    new LambdaQueryWrapper<ReportShare>()
                            .eq(ReportShare::getShareToken, token)
            );

            if (count == 0) {
                return token;
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成分享 Token 失败");
    }

    private ReportShareVO toShareVO(Long reportId, ReportShare share) {
        String shareToken = share == null ? null : share.getShareToken();

        return ReportShareVO.builder()
                .reportId(reportId)
                .shareToken(shareToken)
                .shareUrl(shareToken == null || shareToken.isBlank() ? null : "/share/reports/" + shareToken)
                .enabled(share != null && Integer.valueOf(1).equals(share.getEnabled()))
                .expireTime(share == null ? null : share.getExpireTime())
                .build();
    }

    private PublicReportVO toPublicReportVO(Project project, AnalysisReport report) {
        return PublicReportVO.builder()
                .projectName(project.getName())
                .projectType(project.getProjectType())
                .techStack(project.getTechStack())
                .totalScore(report.getTotalScore())
                .runnabilityScore(report.getRunnabilityScore())
                .authenticityScore(report.getAuthenticityScore())
                .structureScore(report.getStructureScore())
                .readmeScore(report.getReadmeScore())
                .securityScore(report.getSecurityScore())
                .engineeringScore(report.getEngineeringScore())
                .interviewScore(report.getInterviewScore())
                .summary(report.getSummary())
                .strengths(report.getStrengths())
                .weaknesses(report.getWeaknesses())
                .riskPoints(report.getRiskPoints())
                .evidenceChain(report.getEvidenceChain())
                .claimEvidenceList(claimEvidenceAuditService.parseItems(report.getClaimEvidence()))
                .suggestions(report.getSuggestions())
                .resumeBasic(report.getResumeBasic())
                .resumeStandard(report.getResumeStandard())
                .resumeAdvanced(report.getResumeAdvanced())
                .createTime(report.getCreateTime())
                .build();
    }
}
