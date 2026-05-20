package com.xinzhe.projectmentor.hallucination.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.hallucination.dto.HallucinationCheckRequest;
import com.xinzhe.projectmentor.hallucination.vo.HallucinationCheckResultVO;
import com.xinzhe.projectmentor.hallucination.vo.HallucinationIssueVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class HallucinationCheckService {

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

    private final LlmClient llmClient;

    private final HallucinationPromptBuilder hallucinationPromptBuilder;

    private final AiJsonUtil aiJsonUtil;

    public HallucinationCheckResultVO check(HallucinationCheckRequest request) {
        String aiAnswer = request.getAiAnswer();
        String answerLower = aiAnswer.toLowerCase(Locale.ROOT);

        List<ProjectFile> projectFiles = new ArrayList<>();

        if (request.getProjectId() != null) {
            checkProjectOwner(request.getProjectId());
            projectFiles = projectFileMapper.selectList(
                    new LambdaQueryWrapper<ProjectFile>()
                            .eq(ProjectFile::getProjectId, request.getProjectId())
            );
        }

        String evidenceText = buildEvidenceText(projectFiles);

        List<HallucinationIssueVO> issues = new ArrayList<>();
        List<String> unsafeStatements = new ArrayList<>();

        checkOverEncouragement(answerLower, issues, unsafeStatements);
        checkEnterpriseOverclaim(answerLower, evidenceText, issues, unsafeStatements);
        checkHighConcurrencyOverclaim(answerLower, evidenceText, issues, unsafeStatements);
        checkMicroserviceOverclaim(answerLower, evidenceText, issues, unsafeStatements);
        checkRagOverclaim(answerLower, evidenceText, issues, unsafeStatements);
        checkDockerOverclaim(answerLower, evidenceText, issues, unsafeStatements);
        checkBigCompanyGuarantee(answerLower, issues, unsafeStatements);
        checkUnsuitableSuggestions(answerLower, issues, unsafeStatements);

        int highCount = countByRiskLevel(issues, "HIGH");
        int mediumCount = countByRiskLevel(issues, "MEDIUM");

        int credibilityScore = calculateCredibilityScore(highCount, mediumCount, issues.size());
        int objectivityScore = calculateObjectivityScore(answerLower, highCount, mediumCount);

        String riskLevel = determineRiskLevel(highCount, mediumCount, issues.size());

        HallucinationCheckResultVO ruleResult = HallucinationCheckResultVO.builder()
                .credibilityScore(credibilityScore)
                .objectivityScore(objectivityScore)
                .riskLevel(riskLevel)
                .overEncouragementRisk(hasIssueType(issues, "OVER_ENCOURAGEMENT"))
                .missingEvidenceRisk(hasIssueType(issues, "MISSING_EVIDENCE") || hasIssueType(issues, "TECH_OVERCLAIM"))
                .resumeRisk(hasIssueType(issues, "RESUME_RISK") || !unsafeStatements.isEmpty())
                .issueCount(issues.size())
                .issues(issues)
                .unsafeResumeStatements(unsafeStatements)
                .saferRewrite(buildSaferRewrite(issues, request.getProjectId() != null))
                .build();

        return enhanceWithAi(aiAnswer, projectFiles, ruleResult);
    }

    private void checkProjectOwner(Long projectId) {
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限检测");
        }
    }

    private void checkOverEncouragement(String answerLower,
                                        List<HallucinationIssueVO> issues,
                                        List<String> unsafeStatements) {
        List<String> keywords = List.of(
                "完全可以应对大厂面试",
                "非常完美",
                "毫无问题",
                "没有明显缺点",
                "竞争力很强",
                "直接写进简历",
                "大厂级别",
                "顶级项目"
        );

        String matched = firstMatched(answerLower, keywords);

        if (matched != null) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("HIGH")
                    .issueType("OVER_ENCOURAGEMENT")
                    .matchedText(matched)
                    .message("AI 回答出现明显鼓励式或保证式表述，但没有给出足够项目证据，容易让用户高估项目水平。")
                    .evidence("检测到“" + matched + "”等绝对化或过度乐观表达")
                    .suggestion("建议改成基于证据的条件性判断，例如“如果补充运行文档和核心代码证据，可以作为实习项目展示”。")
                    .build());

            unsafeStatements.add("完全可以应对大厂面试 / 大厂级别 / 顶级项目 等保证式表述");
        }
    }

    private void checkEnterpriseOverclaim(String answerLower,
                                          String evidenceText,
                                          List<HallucinationIssueVO> issues,
                                          List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("企业级", "生产级", "高可用", "99.99", "核心系统"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "globalexceptionhandler", "result", "knife4j", "swagger",
                "dockerfile", "docker-compose", "init.sql", "log", "monitor"
        ));

        if (!hasEvidence) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("HIGH")
                    .issueType("TECH_OVERCLAIM")
                    .matchedText(matched)
                    .message("AI 回答中使用了“" + matched + "”等包装化表述，但当前项目证据不足。")
                    .evidence("未发现足够工程化证据，例如全局异常、统一返回、接口文档、部署文件、初始化 SQL、日志或监控")
                    .suggestion("建议避免写“企业级/生产级”，改成具体功能描述。")
                    .build());

            unsafeStatements.add("企业级 / 生产级 / 高可用 / 核心系统");
        }
    }

    private void checkHighConcurrencyOverclaim(String answerLower,
                                               String evidenceText,
                                               List<HallucinationIssueVO> issues,
                                               List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("高并发", "1000qps", "千万级", "大规模", "秒杀"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "jmeter", "wrk", "压测", "ratelimiter", "sentinel",
                "redis", "redistemplate", "@async", "threadpoolexecutor"
        ));

        if (!hasEvidence) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("HIGH")
                    .issueType("RESUME_RISK")
                    .matchedText(matched)
                    .message("AI 回答中出现“" + matched + "”等高并发表述，但没有压测、限流、缓存或异步处理证据。")
                    .evidence("未发现 JMeter/wrk 压测报告、限流组件、Redis 缓存、@Async 或线程池实现")
                    .suggestion("不要把该表述直接写入简历，除非补充真实实现和测试数据。")
                    .build());

            unsafeStatements.add("高并发 / 1000QPS / 千万级 / 秒杀");
        }
    }

    private void checkMicroserviceOverclaim(String answerLower,
                                            String evidenceText,
                                            List<HallucinationIssueVO> issues,
                                            List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("微服务", "spring cloud", "nacos", "gateway", "openfeign", "dubbo"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "spring-cloud", "nacos", "gateway", "openfeign", "dubbo", "eureka"
        ));

        if (!hasEvidence) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("HIGH")
                    .issueType("TECH_OVERCLAIM")
                    .matchedText(matched)
                    .message("AI 回答建议或声称项目具备微服务能力，但未发现注册中心、网关、远程调用或多服务模块证据。")
                    .evidence("未发现 Spring Cloud / Nacos / Gateway / OpenFeign / Dubbo 等证据")
                    .suggestion("如果项目是单体应用，应明确写成“模块化单体”，不要包装成微服务。")
                    .build());

            unsafeStatements.add("微服务架构 / Spring Cloud / Nacos / Gateway");
        }
    }

    private void checkRagOverclaim(String answerLower,
                                   String evidenceText,
                                   List<HallucinationIssueVO> issues,
                                   List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("rag", "向量检索", "embedding", "知识库问答", "相似度检索"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "embedding", "chunk", "vector", "topk", "similarity", "cosine", "milvus", "pgvector"
        ));

        if (!hasEvidence) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("MEDIUM")
                    .issueType("MISSING_EVIDENCE")
                    .matchedText(matched)
                    .message("AI 回答中提到 RAG 或向量检索能力，但当前证据不足，可能只是调用大模型 API。")
                    .evidence("未发现 chunk、embedding、vector、similarity、cosine、Milvus、PgVector 等相关证据")
                    .suggestion("如果没有完整检索增强流程，不建议写成 RAG；可以改为“大模型 API 调用”。")
                    .build());

            unsafeStatements.add("RAG / 向量检索 / 知识库问答");
        }
    }

    private void checkDockerOverclaim(String answerLower,
                                      String evidenceText,
                                      List<HallucinationIssueVO> issues,
                                      List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("docker", "容器化", "docker-compose"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "dockerfile", "docker-compose.yml", "docker-compose.yaml"
        ));

        if (!hasEvidence) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("MEDIUM")
                    .issueType("MISSING_EVIDENCE")
                    .matchedText(matched)
                    .message("AI 回答中提到 Docker 或容器化，但当前项目证据中未发现 Dockerfile 或 docker-compose 文件。")
                    .evidence("未发现 Dockerfile / docker-compose.yml / docker-compose.yaml")
                    .suggestion("如果尚未实现 Docker 部署，不要在简历中写“支持 Docker 部署”。")
                    .build());

            unsafeStatements.add("Docker 容器化部署");
        }
    }

    private void checkBigCompanyGuarantee(String answerLower,
                                          List<HallucinationIssueVO> issues,
                                          List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("大厂面试稳了", "一定能过", "保底", "稳进", "大厂 offer"));

        if (matched != null) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("HIGH")
                    .issueType("OVER_ENCOURAGEMENT")
                    .matchedText(matched)
                    .message("AI 回答中出现结果保证式表述，这类表述不客观，也不适合用于项目评估。")
                    .evidence("检测到“" + matched + "”等承诺式表达")
                    .suggestion("应改为基于当前项目证据和面试准备程度的风险评估。")
                    .build());

            unsafeStatements.add("一定能过 / 稳进 / 保底 / 大厂 Offer");
        }
    }

    private void checkUnsuitableSuggestions(String answerLower,
                                            List<HallucinationIssueVO> issues,
                                            List<String> unsafeStatements) {
        String matched = firstMatched(answerLower, List.of("建议改成微服务", "上 kubernetes", "上 k8s", "引入 nacos", "引入 spring cloud"));

        if (matched != null) {
            issues.add(HallucinationIssueVO.builder()
                    .riskLevel("MEDIUM")
                    .issueType("UNSUITABLE_SUGGESTION")
                    .matchedText(matched)
                    .message("AI 给出了可能不适合当前学生项目阶段的过度架构建议。")
                    .evidence("检测到“" + matched + "”等复杂架构建议")
                    .suggestion("对于早期学生项目，应优先补充可运行性、README、初始化 SQL、异常处理、接口文档和真实业务闭环，而不是过早引入微服务或 Kubernetes。")
                    .build());

            unsafeStatements.add("过早引入微服务 / Kubernetes / Spring Cloud");
        }
    }

    private String buildEvidenceText(List<ProjectFile> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (ProjectFile file : files) {
            if (file.getFilePath() != null) {
                builder.append(file.getFilePath()).append("\n");
            }
            if (file.getContent() != null) {
                builder.append(file.getContent()).append("\n");
            }
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private String firstMatched(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return keyword;
            }
        }
        return null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int countByRiskLevel(List<HallucinationIssueVO> issues, String riskLevel) {
        return (int) issues.stream()
                .filter(issue -> riskLevel.equalsIgnoreCase(issue.getRiskLevel()))
                .count();
    }

    private boolean hasIssueType(List<HallucinationIssueVO> issues, String issueType) {
        return issues.stream()
                .anyMatch(issue -> issueType.equalsIgnoreCase(issue.getIssueType()));
    }

    private int calculateCredibilityScore(int highCount, int mediumCount, int issueCount) {
        int score = 100;
        score -= highCount * 18;
        score -= mediumCount * 10;
        score -= Math.max(0, issueCount - highCount - mediumCount) * 4;
        return clamp(score);
    }

    private int calculateObjectivityScore(String answerLower, int highCount, int mediumCount) {
        int score = 90;

        if (containsAny(answerLower, List.of("完全", "一定", "稳了", "顶级", "毫无问题", "大厂级别"))) {
            score -= 25;
        }

        score -= highCount * 10;
        score -= mediumCount * 5;

        return clamp(score);
    }

    private String determineRiskLevel(int highCount, int mediumCount, int issueCount) {
        if (highCount >= 2) {
            return "HIGH";
        }

        if (highCount >= 1 || mediumCount >= 2 || issueCount >= 3) {
            return "MEDIUM";
        }

        if (issueCount > 0) {
            return "LOW";
        }

        return "LOW";
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private HallucinationCheckResultVO enhanceWithAi(String aiAnswer,
                                                    List<ProjectFile> projectFiles,
                                                    HallucinationCheckResultVO ruleResult) {
        try {
            String content = llmClient.chat(
                    "HALLUCINATION",
                    hallucinationPromptBuilder.buildSystemPrompt(),
                    hallucinationPromptBuilder.buildUserPrompt(aiAnswer, projectFiles, ruleResult)
            );

            HallucinationCheckResultVO aiResult = parseAiResult(content, ruleResult);
            return aiResult == null ? ruleResult : aiResult;
        } catch (Exception e) {
            log.info("AI hallucination enhancement unavailable, fallback to rule result: {}", e.getMessage());
            return ruleResult;
        }
    }

    private HallucinationCheckResultVO parseAiResult(String content, HallucinationCheckResultVO ruleResult) {
        JsonNode root = aiJsonUtil.safeReadTree(content);
        if (root == null || !root.isObject()) {
            return null;
        }

        Integer credibilityScore = getScore(root, "credibilityScore");
        Integer objectivityScore = getScore(root, "objectivityScore");
        String riskLevel = normalizeRiskLevel(aiJsonUtil.getText(root, "riskLevel"));
        List<HallucinationIssueVO> aiIssues = parseIssues(root.get("issues"));

        if (credibilityScore == null || objectivityScore == null || riskLevel == null || aiIssues == null) {
            return null;
        }

        List<HallucinationIssueVO> mergedIssues = mergeIssues(ruleResult.getIssues(), aiIssues);
        List<String> mergedUnsafeStatements = mergeStrings(
                ruleResult.getUnsafeResumeStatements(),
                parseStringArray(root.get("unsafeResumeStatements"))
        );

        int ruleHighCount = countByRiskLevel(ruleResult.getIssues(), "HIGH");
        if (ruleHighCount > 0) {
            credibilityScore = Math.min(credibilityScore, ruleResult.getCredibilityScore());
            objectivityScore = Math.min(objectivityScore, ruleResult.getObjectivityScore());
        }

        String finalRiskLevel = maxRiskLevel(riskLevel, ruleResult.getRiskLevel());
        if (countByRiskLevel(mergedIssues, "HIGH") > 0) {
            finalRiskLevel = maxRiskLevel(finalRiskLevel, "HIGH");
        }

        String saferRewrite = aiJsonUtil.getText(root, "saferRewrite");
        if (saferRewrite.isBlank()) {
            saferRewrite = ruleResult.getSaferRewrite();
        }

        return HallucinationCheckResultVO.builder()
                .credibilityScore(clamp(credibilityScore))
                .objectivityScore(clamp(objectivityScore))
                .riskLevel(finalRiskLevel)
                .overEncouragementRisk(hasIssueType(mergedIssues, "OVER_ENCOURAGEMENT"))
                .missingEvidenceRisk(hasIssueType(mergedIssues, "MISSING_EVIDENCE")
                        || hasIssueType(mergedIssues, "TECH_OVERCLAIM"))
                .resumeRisk(hasIssueType(mergedIssues, "RESUME_RISK") || !mergedUnsafeStatements.isEmpty())
                .issueCount(mergedIssues.size())
                .issues(mergedIssues)
                .unsafeResumeStatements(mergedUnsafeStatements)
                .saferRewrite(saferRewrite)
                .build();
    }

    private Integer getScore(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }

        int score;
        if (node.isInt()) {
            score = node.asInt();
        } else if (node.isTextual()) {
            try {
                score = Integer.parseInt(node.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        if (score < 0 || score > 100) {
            return null;
        }

        return score;
    }

    private List<HallucinationIssueVO> parseIssues(JsonNode issuesNode) {
        if (issuesNode == null || !issuesNode.isArray()) {
            return null;
        }

        List<HallucinationIssueVO> issues = new ArrayList<>();
        for (JsonNode node : issuesNode) {
            if (!node.isObject()) {
                continue;
            }

            String riskLevel = normalizeRiskLevel(aiJsonUtil.getText(node, "riskLevel"));
            String issueType = aiJsonUtil.getText(node, "issueType");
            String message = aiJsonUtil.getText(node, "message");

            if (riskLevel == null || issueType.isBlank() || message.isBlank()) {
                continue;
            }

            issues.add(HallucinationIssueVO.builder()
                    .riskLevel(riskLevel)
                    .issueType(issueType)
                    .matchedText(aiJsonUtil.getText(node, "matchedText"))
                    .message(message)
                    .evidence(aiJsonUtil.getText(node, "evidence"))
                    .suggestion(aiJsonUtil.getText(node, "suggestion"))
                    .build());
        }

        return issues;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }

        for (JsonNode item : node) {
            String value = item.isTextual() ? item.asText() : item.toString();
            if (!value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }

    private List<HallucinationIssueVO> mergeIssues(List<HallucinationIssueVO> ruleIssues,
                                                   List<HallucinationIssueVO> aiIssues) {
        List<HallucinationIssueVO> merged = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();

        addIssues(aiIssues, merged, keys);
        addIssues(ruleIssues, merged, keys);

        return merged;
    }

    private void addIssues(List<HallucinationIssueVO> source,
                           List<HallucinationIssueVO> target,
                           Set<String> keys) {
        if (source == null) {
            return;
        }

        for (HallucinationIssueVO issue : source) {
            String key = normalize(issue.getRiskLevel()) + "|"
                    + normalize(issue.getIssueType()) + "|"
                    + normalize(issue.getMatchedText()) + "|"
                    + normalize(issue.getMessage());
            if (keys.add(key)) {
                target.add(issue);
            }
        }
    }

    private List<String> mergeStrings(List<String> ruleValues, List<String> aiValues) {
        Set<String> merged = new LinkedHashSet<>();
        if (aiValues != null) {
            aiValues.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(merged::add);
        }
        if (ruleValues != null) {
            ruleValues.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return null;
        }

        String normalized = riskLevel.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> null;
        };
    }

    private String maxRiskLevel(String left, String right) {
        String normalizedLeft = normalizeRiskLevel(left);
        String normalizedRight = normalizeRiskLevel(right);

        if (normalizedLeft == null) {
            return normalizedRight == null ? "LOW" : normalizedRight;
        }

        if (normalizedRight == null) {
            return normalizedLeft;
        }

        return riskRank(normalizedLeft) >= riskRank(normalizedRight) ? normalizedLeft : normalizedRight;
    }

    private int riskRank(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String buildSaferRewrite(List<HallucinationIssueVO> issues, boolean hasProjectContext) {
        if (issues.isEmpty()) {
            return "这段 AI 回答整体较为克制，但仍建议结合项目代码、README、配置文件和运行结果进一步验证，避免把没有证据的功能写进简历。";
        }

        if (hasProjectContext) {
            return "更客观的写法：当前项目已具备一定基础，但具体能否写进简历，需要以代码、README、配置文件和运行结果为证据。对于高并发、微服务、RAG、Docker、Redis 等表述，只有在项目中存在对应实现或配置证据时才建议写入简历；否则应改为“计划支持”或删除相关表述。";
        }

        return "更客观的写法：这段 AI 回答中部分表述偏乐观，建议不要直接采纳“企业级”“高并发”“微服务”“大厂级别”等描述。应先检查项目代码和 README 是否有对应证据，再决定是否写进简历。";
    }
}
