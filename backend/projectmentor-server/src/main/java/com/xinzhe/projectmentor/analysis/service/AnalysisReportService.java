package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisReportListItemVO;
import com.xinzhe.projectmentor.analysis.vo.AnalysisReportVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAiPromptBuilder;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceAiEnhancementVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceAiItemVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.common.PageResult;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.scanner.ProjectRuleScanner;
import com.xinzhe.projectmentor.scanner.vo.EvidenceItemVO;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import com.xinzhe.projectmentor.share.entity.ReportShare;
import com.xinzhe.projectmentor.share.mapper.ReportShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class AnalysisReportService {
    private final com.xinzhe.projectmentor.credit.service.CreditService creditService;

    private final AnalysisReportMapper analysisReportMapper;

    private final ProjectMapper projectMapper;

    private final ReportShareMapper reportShareMapper;

    private final ProjectRuleScanner projectRuleScanner;

    private final ClaimEvidenceAuditService claimEvidenceAuditService;

    private final ObjectMapper objectMapper;

    private final AiJsonUtil aiJsonUtil;

    private final com.xinzhe.projectmentor.ai.LlmClient llmClient;

    private final AuditPromptBuilder auditPromptBuilder;

    private final ClaimEvidenceAiPromptBuilder claimEvidenceAiPromptBuilder;

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
                    CreditCostConstants.ANALYSIS_REPORT,
                    CreditCostConstants.OP_GENERATE_ANALYSIS_REPORT,
                    projectId,
                    "生成项目审计报告消耗 1 点额度"
            );
            creditConsumed = true;

            RuleScanResultVO scanResult = projectRuleScanner.scanProject(projectId);
            ClaimEvidenceVO claimEvidence = claimEvidenceAuditService.audit(project, projectId);

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
            report.setClaimEvidence(toJson(claimEvidence.getItems()));

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
                        CreditCostConstants.ANALYSIS_REPORT,
                        CreditCostConstants.OP_GENERATE_ANALYSIS_REPORT_REFUND,
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

    public PageResult<AnalysisReportListItemVO> listMyReports(Integer page,
                                                              Integer size,
                                                              Long projectId,
                                                              String keyword) {
        Long userId = getCurrentUserId();
        int safePage = sanitizePage(page);
        int safeSize = sanitizeSize(size);
        List<Project> ownedProjects = listOwnedProjects(userId, projectId);

        if (projectId != null && ownedProjects.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Project not found or no permission");
        }

        if (ownedProjects.isEmpty()) {
            return emptyPage(safePage, safeSize);
        }

        Set<Long> ownedProjectIds = ownedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> keywordProjectIds = matchProjectIdsByKeyword(ownedProjects, keyword);

        Long total = analysisReportMapper.selectCount(buildMyReportWrapper(ownedProjectIds, keyword, keywordProjectIds));
        if (total == null || total == 0) {
            return PageResult.<AnalysisReportListItemVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .page(safePage)
                    .size(safeSize)
                    .build();
        }

        int offset = (safePage - 1) * safeSize;
        List<AnalysisReport> reports = analysisReportMapper.selectList(
                buildMyReportWrapper(ownedProjectIds, keyword, keywordProjectIds)
                        .orderByDesc(AnalysisReport::getCreateTime)
                        .orderByDesc(AnalysisReport::getId)
                        .last("LIMIT " + offset + ", " + safeSize)
        );

        Map<Long, Project> projectMap = ownedProjects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left, HashMap::new));
        Map<Long, ReportShare> shareMap = loadShareMap(
                reports.stream().map(AnalysisReport::getId).collect(Collectors.toCollection(LinkedHashSet::new)),
                userId
        );

        return PageResult.<AnalysisReportListItemVO>builder()
                .records(reports.stream()
                        .map(report -> toListItemVO(report, projectMap, shareMap))
                        .toList())
                .total(total)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public List<AnalysisReportListItemVO> listRecentMyReports(Integer limit) {
        return listMyReports(1, sanitizeLimit(limit), null, null).getRecords();
    }

    public Long countMyReports() {
        Long userId = getCurrentUserId();
        List<Project> ownedProjects = listOwnedProjects(userId, null);

        if (ownedProjects.isEmpty()) {
            return 0L;
        }

        return analysisReportMapper.selectCount(new LambdaQueryWrapper<AnalysisReport>()
                .in(AnalysisReport::getProjectId, ownedProjects.stream()
                        .map(Project::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new))));
    }

    public AnalysisReportVO getReportDetail(Long reportId) {
        AnalysisReport report = requireOwnedReport(reportId, getCurrentUserId());
        return toVO(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVO enhanceClaimEvidence(Long reportId) {
        Long userId = getCurrentUserId();
        AnalysisReport report = requireOwnedReport(reportId, userId);
        List<com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO> claimItems =
                claimEvidenceAuditService.parseItems(report.getClaimEvidence());

        if (claimItems.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前报告暂无主张证据矩阵，请先重新生成报告。");
        }

        String prompt = claimEvidenceAiPromptBuilder.build(claimItems);
        boolean creditConsumed = false;
        boolean aiCallCompleted = false;

        try {
            creditService.consumeCredits(
                    userId,
                    CreditCostConstants.AI_CLAIM_EVIDENCE,
                    CreditCostConstants.OP_AI_CLAIM_EVIDENCE,
                    reportId,
                    "AI 深度解读主张证据矩阵"
            );
            creditConsumed = true;

            String content = llmClient.chat(
                    "CLAIM_EVIDENCE",
                    claimEvidenceAiPromptBuilder.systemPrompt(),
                    prompt
            );
            aiCallCompleted = true;
            ClaimEvidenceAiEnhancementVO aiEnhancement = parseClaimEvidenceAiEnhancement(content);
            aiEnhancement.setAiEnhanced(true);
            aiEnhancement.setAiEnhancedAt(LocalDateTime.now());

            report.setClaimEvidence(mergeClaimEvidenceAiEnhancement(
                    report.getClaimEvidence(),
                    claimItems,
                    aiEnhancement
            ));

            int updated = analysisReportMapper.updateById(report);
            if (updated <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 增强结果保存失败，额度已返还，请稍后重试。");
            }

            return toVO(report);
        } catch (BusinessException e) {
            if (creditConsumed) {
                refundClaimEvidenceCredits(userId, reportId);
            }

            if (e.getCode() == ErrorCode.CREDIT_NOT_ENOUGH.getCode()) {
                throw e;
            }
            if (e.getCode() == ErrorCode.AI_SERVICE_ERROR.getCode()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 调用失败，额度已返还，请稍后重试。");
            }
            if (aiCallCompleted) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 解读已生成但保存失败，额度已返还，请稍后重试。");
            }

            throw e;
        } catch (Exception e) {
            if (creditConsumed) {
                refundClaimEvidenceCredits(userId, reportId);
            }

            if (aiCallCompleted) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 解读已生成但保存失败，额度已返还，请稍后重试。");
            }

            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 调用失败，额度已返还，请稍后重试。");
        }
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

    private AnalysisReport requireOwnedReport(Long reportId, Long userId) {
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

        return report;
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private List<Project> listOwnedProjects(Long userId, Long projectId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .select(Project::getId, Project::getName, Project::getTechStack, Project::getCreateTime)
                .eq(Project::getUserId, userId);

        if (projectId != null) {
            wrapper.eq(Project::getId, projectId);
        }

        return projectMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<AnalysisReport> buildMyReportWrapper(Set<Long> projectIds,
                                                                    String keyword,
                                                                    Set<Long> keywordProjectIds) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<AnalysisReport>()
                .in(AnalysisReport::getProjectId, projectIds);

        if (!StringUtils.hasText(keyword)) {
            return wrapper;
        }

        String normalizedKeyword = keyword.trim();
        wrapper.and(query -> {
            query.like(AnalysisReport::getSummary, normalizedKeyword);
            if (keywordProjectIds != null && !keywordProjectIds.isEmpty()) {
                query.or().in(AnalysisReport::getProjectId, keywordProjectIds);
            }
        });

        return wrapper;
    }

    private Set<Long> matchProjectIdsByKeyword(List<Project> projects, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }

        String lowerKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return projects.stream()
                .filter(project -> project.getName() != null
                        && project.getName().toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .map(Project::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, ReportShare> loadShareMap(Set<Long> reportIds, Long userId) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return reportShareMapper.selectList(new LambdaQueryWrapper<ReportShare>()
                        .in(ReportShare::getReportId, reportIds)
                        .eq(ReportShare::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(
                        ReportShare::getReportId,
                        Function.identity(),
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private PageResult<AnalysisReportListItemVO> emptyPage(int page, int size) {
        return PageResult.<AnalysisReportListItemVO>builder()
                .records(Collections.emptyList())
                .total(0L)
                .page(page)
                .size(size)
                .build();
    }

    private int sanitizePage(Integer page) {
        return Math.max(1, page == null ? 1 : page);
    }

    private int sanitizeSize(Integer size) {
        int safeSize = size == null ? 10 : size;
        return Math.max(1, Math.min(safeSize, 50));
    }

    private int sanitizeLimit(Integer limit) {
        int safeLimit = limit == null ? 5 : limit;
        return Math.max(1, Math.min(safeLimit, 20));
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
        String evidenceSources = buildEvidenceSources(scanResult);
        String risk = buildResumeRisk(scanResult);
        String description = "基于 " + safe(project.getTechStack()) + " 参与/整理“" + project.getName()
                + "”，围绕" + safeProjectScope(project)
                + "沉淀 README、项目结构和可验证材料；当前可优先表达已有文件证据能支撑的基础工作。";

        return formatResumeVersion(
                "保守投递、证据还不完整或需要先保证可解释时使用。",
                description,
                risk,
                "为什么选择这些技术栈？哪些功能已有文件证据？哪些内容只是 README 描述或计划？",
                evidenceSources
        );
    }

    private String buildResumeStandard(Project project, RuleScanResultVO scanResult) {
        String evidenceSources = buildEvidenceSources(scanResult);
        String risk = buildResumeRisk(scanResult);
        String description = "基于 " + safe(project.getTechStack()) + " 打磨“" + project.getName()
                + "”，围绕" + safeProjectScope(project)
                + "完成可展示的项目材料和实现边界梳理，并结合已上传代码/配置/文档证据说明项目能力与风险。";

        return formatResumeVersion(
                "适合作为简历主体描述，但仍需要按真实贡献调整措辞。",
                description,
                risk,
                "项目核心流程如何跑通？关键文件分别承担什么职责？证据不足的能力为什么没有写成核心实现？",
                evidenceSources
        );
    }

    private String buildResumeAdvanced(Project project, RuleScanResultVO scanResult) {
        String evidenceSources = buildEvidenceSources(scanResult);
        boolean highRisk = scanResult.getHighRiskCount() != null && scanResult.getHighRiskCount() > 0;
        boolean hasCodeEvidence = hasCodeEvidence(scanResult);
        String risk = buildResumeRisk(scanResult);
        String description;

        if (highRisk || !hasCodeEvidence) {
            description = "当前不建议把“" + project.getName()
                    + "”写成核心实现亮点。可以把它作为项目复盘、工程化材料整理或功能边界说明来讲，先补充真实代码证据后再升级表达。";
        } else {
            description = "在“" + project.getName()
                    + "”中围绕" + safeProjectScope(project)
                    + "梳理关键实现、证据来源和面试解释路径，可在面试中延展说明设计取舍、实现流程和风险边界。";
        }

        return formatResumeVersion(
                "仅适合面试延展或项目讲解，不建议直接照搬成夸张简历亮点。",
                description,
                risk,
                "哪些文件能证明核心流程？哪些技术点可以现场解释？如果被追问实现细节，哪些边界需要主动说明？",
                evidenceSources
        );
    }

    private String formatResumeVersion(String scenario,
                                       String description,
                                       String risk,
                                       String followUp,
                                       String evidenceSources) {
        return """
                推荐使用场景：%s
                描述：%s
                风险提示：%s
                可被追问点：%s
                证据来源：%s
                """.formatted(scenario, description, risk, followUp, evidenceSources).trim();
    }

    private String safeProjectScope(Project project) {
        if (!isBlank(project.getDescription())) {
            return "“" + project.getDescription().trim() + "”这一项目目标，";
        }

        if (!isBlank(project.getProjectType())) {
            return project.getProjectType().trim() + "项目方向，";
        }

        return "项目基础功能，";
    }

    private String buildResumeRisk(RuleScanResultVO scanResult) {
        if (!hasCodeEvidence(scanResult)) {
            return "当前代码证据不足，不建议写成核心实现；如果只有 README、Docker 或配置证据，需要在面试中主动说明边界。";
        }

        if (scanResult.getHighRiskCount() != null && scanResult.getHighRiskCount() > 0) {
            return "当前仍有 HIGH 风险，请先降低 README 或简历中的确定性表达，避免把缺证据能力写成已完整实现。";
        }

        if (scanResult.getTotalRiskCount() != null && scanResult.getTotalRiskCount() > 0) {
            return "存在部分证据不足或表述风险，建议复制后结合个人真实贡献继续收敛。";
        }

        return "当前未发现明显高风险表述，但仍建议只写自己能解释清楚、能指向文件证据的内容。";
    }

    private String buildEvidenceSources(RuleScanResultVO scanResult) {
        if (scanResult.getEvidences() == null || scanResult.getEvidences().isEmpty()) {
            return "暂无明确证据来源。";
        }

        java.util.LinkedHashSet<String> sources = new java.util.LinkedHashSet<>();
        for (EvidenceItemVO evidence : scanResult.getEvidences()) {
            if (sources.size() >= 4) {
                break;
            }

            String sourceFile = evidence.getSourceFile();
            if (isBlank(sourceFile)) {
                continue;
            }

            String conclusion = isBlank(evidence.getConclusion()) ? "相关证据" : evidence.getConclusion();
            sources.add(sourceFile + "（" + conclusion + "）");
        }

        if (sources.isEmpty()) {
            return "暂无明确证据来源。";
        }

        return String.join("；", sources);
    }

    private boolean hasCodeEvidence(RuleScanResultVO scanResult) {
        if (scanResult.getEvidences() == null) {
            return false;
        }

        return scanResult.getEvidences().stream()
                .map(EvidenceItemVO::getSourceFile)
                .filter(sourceFile -> sourceFile != null)
                .map(sourceFile -> sourceFile.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(sourceFile -> sourceFile.endsWith(".java")
                        || sourceFile.contains("/src/")
                        || sourceFile.contains("controller")
                        || sourceFile.contains("service")
                        || sourceFile.contains("mapper")
                        || sourceFile.contains("entity")
                        || sourceFile.contains("util"));
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

    private AnalysisReportListItemVO toListItemVO(AnalysisReport report,
                                                  Map<Long, Project> projectMap,
                                                  Map<Long, ReportShare> shareMap) {
        Project project = projectMap.get(report.getProjectId());
        ReportShare share = shareMap.get(report.getId());
        boolean shared = share != null && Integer.valueOf(1).equals(share.getEnabled());

        return AnalysisReportListItemVO.builder()
                .reportId(report.getId())
                .projectId(report.getProjectId())
                .projectName(project == null ? null : project.getName())
                .authenticityScore(report.getAuthenticityScore())
                .healthScore(report.getTotalScore())
                .totalScore(report.getTotalScore())
                .status("FINISHED")
                .createTime(report.getCreateTime())
                .updateTime(report.getCreateTime())
                .shared(shared)
                .shareToken(shared ? share.getShareToken() : null)
                .summary(trimSummary(report.getSummary()))
                .build();
    }

    private String trimSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return summary;
        }

        String normalized = summary.trim();
        if (normalized.length() <= 180) {
            return normalized;
        }

        return normalized.substring(0, 180) + "...";
    }

    private ClaimEvidenceAiEnhancementVO parseClaimEvidenceAiEnhancement(String content) {
        JsonNode root = aiJsonUtil.safeReadTree(content);
        if (root == null || !root.isObject()) {
            return fallbackClaimEvidenceAiEnhancement(content);
        }

        return ClaimEvidenceAiEnhancementVO.builder()
                .aiSummary(aiJsonUtil.getText(root, "summary"))
                .aiRiskOverview(aiJsonUtil.getText(root, "riskOverview"))
                .aiResumeStrategy(aiJsonUtil.getText(root, "resumeStrategy"))
                .aiInterviewStrategy(aiJsonUtil.getText(root, "interviewStrategy"))
                .aiEnhancedItems(parseClaimEvidenceAiItems(root.get("items")))
                .build();
    }

    private List<ClaimEvidenceAiItemVO> parseClaimEvidenceAiItems(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }

        List<ClaimEvidenceAiItemVO> items = new ArrayList<>();
        for (JsonNode itemNode : node) {
            items.add(ClaimEvidenceAiItemVO.builder()
                    .claimText(aiJsonUtil.getText(itemNode, "claimText"))
                    .aiExplanation(aiJsonUtil.getText(itemNode, "aiExplanation"))
                    .saferResumeExpression(aiJsonUtil.getText(itemNode, "saferResumeExpression"))
                    .likelyInterviewQuestions(parseTextList(itemNode.get("likelyInterviewQuestions")))
                    .improvementSuggestion(aiJsonUtil.getText(itemNode, "improvementSuggestion"))
                    .build());
        }

        return items;
    }

    private List<String> parseTextList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull()) {
                values.add(item.asText(""));
            }
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private ClaimEvidenceAiEnhancementVO fallbackClaimEvidenceAiEnhancement(String content) {
        return ClaimEvidenceAiEnhancementVO.builder()
                .aiSummary("AI 已返回解读内容，但未能解析为结构化 JSON。")
                .aiRiskOverview("请结合基础主张证据矩阵人工复核，避免直接采用不确定表述。")
                .aiResumeStrategy("优先使用基础矩阵中的保守简历建议，并核对每条 claim 的证据状态。")
                .aiInterviewStrategy("围绕已有证据文件准备解释，遇到缺少证据的主张应主动说明边界。")
                .aiEnhancedItems(Collections.emptyList())
                .aiFallbackText(truncate(content, 4000))
                .build();
    }

    private String mergeClaimEvidenceAiEnhancement(String existingJson,
                                                   List<com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO> items,
                                                   ClaimEvidenceAiEnhancementVO aiEnhancement) {
        try {
            JsonNode rootNode = null;
            if (StringUtils.hasText(existingJson)) {
                rootNode = objectMapper.readTree(existingJson);
            }
            ObjectNode root = rootNode != null && rootNode.isObject()
                    ? (ObjectNode) rootNode.deepCopy()
                    : objectMapper.createObjectNode();

            root.set("items", objectMapper.valueToTree(items));
            root.put("aiEnhanced", Boolean.TRUE.equals(aiEnhancement.getAiEnhanced()));
            root.put("aiEnhancedAt", aiEnhancement.getAiEnhancedAt() == null
                    ? LocalDateTime.now().toString()
                    : aiEnhancement.getAiEnhancedAt().toString());
            root.put("aiSummary", emptyIfNull(aiEnhancement.getAiSummary()));
            root.put("aiRiskOverview", emptyIfNull(aiEnhancement.getAiRiskOverview()));
            root.put("aiResumeStrategy", emptyIfNull(aiEnhancement.getAiResumeStrategy()));
            root.put("aiInterviewStrategy", emptyIfNull(aiEnhancement.getAiInterviewStrategy()));
            root.set("aiEnhancedItems", objectMapper.valueToTree(
                    aiEnhancement.getAiEnhancedItems() == null
                            ? Collections.emptyList()
                            : aiEnhancement.getAiEnhancedItems()
            ));

            if (StringUtils.hasText(aiEnhancement.getAiFallbackText())) {
                root.put("aiFallbackText", aiEnhancement.getAiFallbackText());
            } else {
                root.remove("aiFallbackText");
            }

            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 增强结果序列化失败");
        }
    }

    private void refundClaimEvidenceCredits(Long userId, Long reportId) {
        creditService.refundCredits(
                userId,
                CreditCostConstants.AI_CLAIM_EVIDENCE,
                CreditCostConstants.OP_AI_CLAIM_EVIDENCE_REFUND,
                reportId,
                "AI Claim-Evidence 增强失败返还"
        );
    }

    private String emptyIfNull(String text) {
        return text == null ? "" : text;
    }

    private String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }

        return text.substring(0, maxChars);
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
                .claimEvidenceList(claimEvidenceAuditService.parseItems(report.getClaimEvidence()))
                .claimEvidenceAi(claimEvidenceAuditService.parseAiEnhancement(report.getClaimEvidence()))
                .suggestions(report.getSuggestions())
                .resumeBasic(report.getResumeBasic())
                .resumeStandard(report.getResumeStandard())
                .resumeAdvanced(report.getResumeAdvanced())
                .createTime(report.getCreateTime())
                .build();
    }
}
