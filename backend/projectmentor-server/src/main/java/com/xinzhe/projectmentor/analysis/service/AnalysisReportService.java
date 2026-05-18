package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisReportVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.scanner.ProjectRuleScanner;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisReportService {
    private final com.xinzhe.projectmentor.credit.service.CreditService creditService;

    private final AnalysisReportMapper analysisReportMapper;

    private final ProjectMapper projectMapper;

    private final ProjectRuleScanner projectRuleScanner;

    private final ObjectMapper objectMapper;

    private final com.xinzhe.projectmentor.ai.LlmClient llmClient;

    private final AuditPromptBuilder auditPromptBuilder;

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVO generateReport(Long projectId) {
        Project project = checkProjectOwner(projectId);
        Long userId = UserContext.getUserId();

        boolean creditConsumed = false;

        try {
            creditService.consumeCredits(
                    userId,
                    1,
                    "GENERATE_ANALYSIS_REPORT",
                    projectId,
                    "生成项目审计报告消耗 1 点额度"
            );
            creditConsumed = true;

            RuleScanResultVO scanResult = projectRuleScanner.scanProject(projectId);

            int authenticityScore = calculateAuthenticityScore(scanResult);
            int readmeScore = calculateReadmeScore(scanResult);
            int structureScore = calculateStructureScore(scanResult);
            int engineeringScore = calculateEngineeringScore(scanResult);
            int securityScore = calculateSecurityScore(scanResult);
            int runnabilityScore = calculateRunnabilityScore(scanResult);
            int interviewScore = calculateInterviewScore(scanResult);

            int totalScore = calculateTotalScore(
                    runnabilityScore,
                    authenticityScore,
                    structureScore,
                    securityScore,
                    engineeringScore,
                    interviewScore,
                    readmeScore
            );

            AnalysisReport report = new AnalysisReport();
            report.setProjectId(projectId);
            report.setTotalScore(totalScore);
            report.setRunnabilityScore(runnabilityScore);
            report.setAuthenticityScore(authenticityScore);
            report.setStructureScore(structureScore);
            report.setReadmeScore(readmeScore);
            report.setSecurityScore(securityScore);
            report.setEngineeringScore(engineeringScore);
            report.setInterviewScore(interviewScore);
            report.setRiskPoints(toJson(scanResult.getRisks()));
            report.setEvidenceChain(toJson(scanResult.getEvidences()));

            String fallbackSummary = buildSummary(project, totalScore, scanResult);
            String fallbackStrengths = buildStrengths(scanResult);
            String fallbackWeaknesses = buildWeaknesses(scanResult);
            String fallbackSuggestions = toJson(scanResult.getSuggestions());
            String fallbackResumeBasic = buildResumeBasic(project, scanResult);
            String fallbackResumeStandard = buildResumeStandard(project, scanResult);
            String fallbackResumeAdvanced = buildResumeAdvanced(project, scanResult);

            try {
                String prompt = auditPromptBuilder.build(project, scanResult);
                com.xinzhe.projectmentor.ai.dto.AiAuditResult aiResult = llmClient.generateAuditReport(prompt);

                report.setSummary(isBlank(aiResult.getSummary()) ? fallbackSummary : aiResult.getSummary());
                report.setStrengths(isBlank(aiResult.getStrengths()) ? fallbackStrengths : aiResult.getStrengths());
                report.setWeaknesses(isBlank(aiResult.getWeaknesses()) ? fallbackWeaknesses : aiResult.getWeaknesses());
                report.setSuggestions(isBlank(aiResult.getSuggestions()) ? fallbackSuggestions : aiResult.getSuggestions());
                report.setResumeBasic(isBlank(aiResult.getResumeBasic()) ? fallbackResumeBasic : aiResult.getResumeBasic());
                report.setResumeStandard(isBlank(aiResult.getResumeStandard()) ? fallbackResumeStandard : aiResult.getResumeStandard());
                report.setResumeAdvanced(isBlank(aiResult.getResumeAdvanced()) ? fallbackResumeAdvanced : aiResult.getResumeAdvanced());
            } catch (Exception e) {
                report.setSummary(fallbackSummary + "（AI 增强分析暂不可用，当前报告由规则扫描模块生成。）");
                report.setStrengths(fallbackStrengths);
                report.setWeaknesses(fallbackWeaknesses);
                report.setSuggestions(fallbackSuggestions);
                report.setResumeBasic(fallbackResumeBasic);
                report.setResumeStandard(fallbackResumeStandard);
                report.setResumeAdvanced(fallbackResumeAdvanced);
            }

            analysisReportMapper.insert(report);

            project.setStatus("FINISHED");
            projectMapper.updateById(project);

            return toVO(report);
        } catch (Exception e) {
            if (creditConsumed) {
                creditService.refundCredits(
                        userId,
                        1,
                        "GENERATE_ANALYSIS_REPORT_REFUND",
                        projectId,
                        "项目审计报告生成失败，返还 1 点额度"
                );
            }

            throw e;
        }
    }

    public List<AnalysisReportVO> listProjectReports(Long projectId) {
        checkProjectOwner(projectId);

        List<AnalysisReport> reports = analysisReportMapper.selectList(
                new LambdaQueryWrapper<AnalysisReport>()
                        .eq(AnalysisReport::getProjectId, projectId)
                        .orderByDesc(AnalysisReport::getCreateTime)
        );

        return reports.stream()
                .map(this::toVO)
                .toList();
    }

    public AnalysisReportVO getReportDetail(Long reportId) {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

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
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在或无权限访问");
        }

        return toVO(report);
    }

    private Project checkProjectOwner(Long projectId) {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限访问");
        }

        return project;
    }

    private int calculateAuthenticityScore(RuleScanResultVO scanResult) {
        int score = 100;
        score -= scanResult.getHighRiskCount() * 15;
        score -= scanResult.getMediumRiskCount() * 8;
        score -= scanResult.getLowRiskCount() * 3;
        return clamp(score);
    }

    private int calculateReadmeScore(RuleScanResultVO scanResult) {
        if (!Boolean.TRUE.equals(scanResult.getHasReadme())) {
            return 20;
        }

        int score = 90;
        score -= scanResult.getHighRiskCount() * 12;
        score -= scanResult.getMediumRiskCount() * 6;
        return clamp(score);
    }

    private int calculateStructureScore(RuleScanResultVO scanResult) {
        int fileCount = scanResult.getFileCount() == null ? 0 : scanResult.getFileCount();

        if (fileCount <= 0) {
            return 20;
        }

        if (fileCount == 1) {
            return 45;
        }

        if (fileCount <= 3) {
            return 60;
        }

        return 75;
    }

    private int calculateEngineeringScore(RuleScanResultVO scanResult) {
        int evidenceCount = scanResult.getEvidences() == null ? 0 : scanResult.getEvidences().size();

        int score = 45 + evidenceCount * 8;

        return clamp(score);
    }

    private int calculateSecurityScore(RuleScanResultVO scanResult) {
        int score = 80;

        boolean hasHighRisk = scanResult.getHighRiskCount() != null && scanResult.getHighRiskCount() > 0;

        if (hasHighRisk) {
            score -= 15;
        }

        return clamp(score);
    }

    private int calculateRunnabilityScore(RuleScanResultVO scanResult) {
        int fileCount = scanResult.getFileCount() == null ? 0 : scanResult.getFileCount();

        if (fileCount <= 0) {
            return 20;
        }

        if (fileCount == 1) {
            return 50;
        }

        return 65;
    }

    private int calculateInterviewScore(RuleScanResultVO scanResult) {
        int score = 80;
        score -= scanResult.getHighRiskCount() * 10;
        score -= scanResult.getMediumRiskCount() * 5;
        return clamp(score);
    }

    private int calculateTotalScore(int runnabilityScore,
                                    int authenticityScore,
                                    int structureScore,
                                    int securityScore,
                                    int engineeringScore,
                                    int interviewScore,
                                    int readmeScore) {
        double total =
                runnabilityScore * 0.15
                        + authenticityScore * 0.15
                        + structureScore * 0.10
                        + securityScore * 0.15
                        + engineeringScore * 0.10
                        + interviewScore * 0.20
                        + readmeScore * 0.10
                        + 60 * 0.05;

        return clamp((int) Math.round(total));
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String buildSummary(Project project, int totalScore, RuleScanResultVO scanResult) {
        if (totalScore >= 80) {
            return "项目“" + project.getName() + "”目前整体可信度较高，README 风险较少，适合作为进一步打磨的实习项目。";
        }

        if (totalScore >= 60) {
            return "项目“" + project.getName() + "”已具备基础展示价值，但 README 表述和项目证据之间仍存在一定差距，需要补充代码、配置或部署证据。";
        }

        return "项目“" + project.getName() + "”当前面试风险较高，README 中的部分技术表述缺少证据支撑，建议先降低夸大描述并补充真实项目文件。";
    }

    private String buildStrengths(RuleScanResultVO scanResult) {
        if (Boolean.TRUE.equals(scanResult.getHasReadme())) {
            return "项目已提供 README 内容，具备进行项目审计和简历优化的基础材料。";
        }

        return "当前项目资料较少，暂未发现明显优势，需要先补充 README 和核心代码文件。";
    }

    private String buildWeaknesses(RuleScanResultVO scanResult) {
        if (scanResult.getTotalRiskCount() == null || scanResult.getTotalRiskCount() == 0) {
            return "当前规则扫描未发现明显 README 夸大风险，但仍建议继续上传 pom.xml、配置文件和核心代码以提升审计准确性。";
        }

        return "当前发现 " + scanResult.getTotalRiskCount() + " 个风险点，其中 HIGH 级风险 "
                + scanResult.getHighRiskCount() + " 个，主要问题是 README 中部分技术表述缺少对应代码或配置证据。";
    }

    private String buildResumeBasic(Project project, RuleScanResultVO scanResult) {
        return "基于 " + safe(project.getTechStack()) + " 开发“" + project.getName()
                + "”，实现项目基础功能，并整理 README 用于说明项目背景、技术栈和使用方式。";
    }

    private String buildResumeStandard(Project project, RuleScanResultVO scanResult) {
        return "基于 " + safe(project.getTechStack()) + " 开发“" + project.getName()
                + "”，围绕项目管理、文档说明和功能展示进行实现，并通过规则扫描识别 README 表述与项目证据之间的一致性风险。";
    }

    private String buildResumeAdvanced(Project project, RuleScanResultVO scanResult) {
        if (scanResult.getHighRiskCount() != null && scanResult.getHighRiskCount() > 0) {
            return "当前项目仍存在较高 README 夸大风险，暂不建议使用冲刺版简历描述；请先补充真实代码证据或降低夸大表述。";
        }

        return "设计并实现“" + project.getName()
                + "”，结合项目文件、README 和规则扫描结果进行项目真实性评估，提升项目展示可信度和面试可解释性。";
    }

    private String safe(String text) {
        return text == null || text.isBlank() ? "相关技术栈" : text;
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报告 JSON 序列化失败");
        }
    }

    private AnalysisReportVO toVO(AnalysisReport report) {
        return AnalysisReportVO.builder()
                .id(report.getId())
                .projectId(report.getProjectId())
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
                .suggestions(report.getSuggestions())
                .resumeBasic(report.getResumeBasic())
                .resumeStandard(report.getResumeStandard())
                .resumeAdvanced(report.getResumeAdvanced())
                .createTime(report.getCreateTime())
                .build();
    }
}