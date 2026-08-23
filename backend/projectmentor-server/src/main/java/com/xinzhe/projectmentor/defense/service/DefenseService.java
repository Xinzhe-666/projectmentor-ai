package com.xinzhe.projectmentor.defense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.claim.ClaimEvidenceStatus;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceFileVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.defense.dto.CreateDefenseSessionRequest;
import com.xinzhe.projectmentor.defense.dto.DefenseAnswerResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseEvidenceReference;
import com.xinzhe.projectmentor.defense.dto.DefenseQuestionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseReviewResult;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionReviewResponse;
import com.xinzhe.projectmentor.defense.dto.SubmitDefenseAnswerRequest;
import com.xinzhe.projectmentor.defense.entity.DefenseAnswer;
import com.xinzhe.projectmentor.defense.entity.DefenseQuestion;
import com.xinzhe.projectmentor.defense.entity.DefenseSession;
import com.xinzhe.projectmentor.defense.mapper.DefenseAnswerMapper;
import com.xinzhe.projectmentor.defense.mapper.DefenseQuestionMapper;
import com.xinzhe.projectmentor.defense.mapper.DefenseSessionMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefenseService {

    public static final String MODE_EVIDENCE_DEFENSE = "EVIDENCE_DEFENSE";

    public static final String STATUS_CREATING = "CREATING";

    public static final String STATUS_ACTIVE = "ACTIVE";

    public static final String STATUS_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String ALIGNMENT_SUPPORTED = "SUPPORTED";

    public static final String ALIGNMENT_PARTIAL = "PARTIAL";

    public static final String ALIGNMENT_INSUFFICIENT = "INSUFFICIENT";

    private static final int MAX_QUESTION_COUNT = 5;

    private static final TypeReference<List<String>> CLAIM_LIST_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<List<DefenseEvidenceReference>> EVIDENCE_LIST_TYPE = new TypeReference<>() {
    };

    private final DefenseSessionMapper defenseSessionMapper;

    private final DefenseQuestionMapper defenseQuestionMapper;

    private final DefenseAnswerMapper defenseAnswerMapper;

    private final ProjectMapper projectMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final ClaimEvidenceAuditService claimEvidenceAuditService;

    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public DefenseSessionResponse createSession(Long projectId, CreateDefenseSessionRequest request) {
        Project project = requireOwnedProject(projectId);
        AnalysisReport report = requireProjectReport(project.getId(), request.getReportId());
        LocalDateTime now = LocalDateTime.now();

        DefenseSession session = new DefenseSession();
        session.setProjectId(projectId);
        session.setReportId(report.getId());
        session.setMode(normalizeMode(request.getMode()));
        session.setStatus(STATUS_CREATING);
        session.setQuestionCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        if (defenseSessionMapper.insert(session) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 会话创建失败，请稍后重试");
        }

        List<DefenseQuestion> questions = generateQuestions(session, report);
        session.setQuestionCount(questions.size());
        session.setStatus(questions.isEmpty() ? STATUS_INSUFFICIENT_DATA : STATUS_ACTIVE);
        session.setUpdatedAt(LocalDateTime.now());

        if (defenseSessionMapper.updateById(session) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 会话状态保存失败，请稍后重试");
        }

        return toSessionResponse(session);
    }

    public List<DefenseQuestionResponse> getQuestions(Long sessionId) {
        DefenseSession session = requireOwnedSession(sessionId);
        return loadQuestionResponses(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public DefenseAnswerResponse submitAnswer(Long questionId, SubmitDefenseAnswerRequest request) {
        DefenseQuestion question = defenseQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Defense 问题不存在或无权限访问");
        }

        DefenseSession session = requireOwnedSession(question.getSessionId());
        if (STATUS_COMPLETED.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 会话已完成，不能继续提交回答");
        }

        Long existingAnswerCount = defenseAnswerMapper.selectCount(
                new LambdaQueryWrapper<DefenseAnswer>()
                        .eq(DefenseAnswer::getQuestionId, questionId)
        );
        if (existingAnswerCount != null && existingAnswerCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该 Defense 问题已经提交过回答");
        }

        String answerText = request.getAnswerText().trim();
        List<String> relatedClaims = parseClaims(question.getRelatedClaims());
        List<DefenseEvidenceReference> relatedEvidence = parseEvidence(question.getRelatedEvidence());
        List<DefenseEvidenceReference> matchedEvidence = relatedEvidence.stream()
                .filter(evidence -> mentionsEvidence(answerText, evidence))
                .toList();
        String alignment = evaluateAlignment(relatedEvidence, matchedEvidence);
        DefenseReviewResult reviewResult = DefenseReviewResult.builder()
                .evidenceAlignment(alignment)
                .summary(reviewSummary(alignment))
                .relatedClaims(relatedClaims)
                .matchedEvidence(matchedEvidence)
                .build();

        DefenseAnswer answer = new DefenseAnswer();
        answer.setQuestionId(questionId);
        answer.setAnswerText(answerText);
        answer.setEvaluationStatus(alignment);
        answer.setReviewResult(toJson(reviewResult));
        answer.setCreatedAt(LocalDateTime.now());

        if (defenseAnswerMapper.insert(answer) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 回答保存失败，请稍后重试");
        }

        completeSessionWhenAllQuestionsAnswered(session);
        return toAnswerResponse(answer);
    }

    public DefenseSessionReviewResponse getSessionReview(Long sessionId) {
        DefenseSession session = requireOwnedSession(sessionId);
        List<DefenseQuestionResponse> questions = loadQuestionResponses(session);
        List<DefenseAnswerResponse> answers = questions.stream()
                .map(DefenseQuestionResponse::getAnswer)
                .filter(Objects::nonNull)
                .toList();

        return DefenseSessionReviewResponse.builder()
                .session(toSessionResponse(session))
                .questions(questions)
                .answeredCount(answers.size())
                .supportedCount(countAlignment(answers, ALIGNMENT_SUPPORTED))
                .partialCount(countAlignment(answers, ALIGNMENT_PARTIAL))
                .insufficientCount(countAlignment(answers, ALIGNMENT_INSUFFICIENT))
                .build();
    }

    private List<DefenseQuestion> generateQuestions(DefenseSession session, AnalysisReport report) {
        List<DefenseQuestion> questions = new ArrayList<>();
        List<ClaimEvidenceItemVO> claims = new ArrayList<>(
                claimEvidenceAuditService.parseItems(report.getClaimEvidence())
        );
        claims.sort(Comparator.comparingInt(this::claimPriority));

        for (ClaimEvidenceItemVO claim : claims) {
            if (questions.size() >= MAX_QUESTION_COUNT || !StringUtils.hasText(claim.getClaimText())) {
                continue;
            }
            questions.add(buildClaimQuestion(session.getId(), claim, questions.size() + 1));
        }

        if (questions.size() < MAX_QUESTION_COUNT) {
            questions.addAll(buildRiskQuestions(
                    session.getId(),
                    report.getRiskPoints(),
                    questions.size(),
                    MAX_QUESTION_COUNT - questions.size()
            ));
        }

        for (DefenseQuestion question : questions) {
            if (defenseQuestionMapper.insert(question) <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 问题保存失败，请稍后重试");
            }
        }

        return questions;
    }

    private DefenseQuestion buildClaimQuestion(Long sessionId, ClaimEvidenceItemVO claim, int sortOrder) {
        DefenseQuestion question = new DefenseQuestion();
        question.setSessionId(sessionId);
        question.setQuestion(questionForClaim(claim));
        question.setCategory(claim.getCategory() == null ? "GENERAL" : claim.getCategory().name());
        question.setRelatedClaims(toJson(List.of(claim.getClaimText())));
        question.setRelatedEvidence(toJson(toEvidenceReferences(claim.getEvidenceFiles())));
        question.setSortOrder(sortOrder);
        return question;
    }

    private List<DefenseQuestion> buildRiskQuestions(Long sessionId,
                                                     String riskPoints,
                                                     int existingCount,
                                                     int remainingCount) {
        if (!StringUtils.hasText(riskPoints) || remainingCount <= 0) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(riskPoints);
            if (root == null || !root.isArray()) {
                return List.of();
            }

            List<DefenseQuestion> questions = new ArrayList<>();
            for (JsonNode risk : root) {
                if (questions.size() >= remainingCount) {
                    break;
                }

                String message = text(risk, "message");
                String riskType = text(risk, "riskType");
                String keyword = text(risk, "keyword");
                String riskSubject = firstNonBlank(message, keyword, riskType);
                if (!StringUtils.hasText(riskSubject)) {
                    continue;
                }

                DefenseQuestion question = new DefenseQuestion();
                question.setSessionId(sessionId);
                question.setQuestion("报告发现以下项目风险：“" + riskSubject
                        + "”。请结合当前项目证据说明该风险是否成立，以及应如何修正实现或项目表述。");
                question.setCategory("RISK");
                question.setRelatedClaims("[]");
                question.setRelatedEvidence(toJson(riskEvidence(risk)));
                question.setSortOrder(existingCount + questions.size() + 1);
                questions.add(question);
            }
            return questions;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<DefenseEvidenceReference> riskEvidence(JsonNode risk) {
        String sourceFile = text(risk, "sourceFile");
        String evidence = text(risk, "evidence");
        if (!StringUtils.hasText(sourceFile) || "-".equals(sourceFile.trim())) {
            return List.of();
        }

        return List.of(DefenseEvidenceReference.builder()
                .filePath(sourceFile)
                .evidenceLevel(text(risk, "riskLevel"))
                .snippet(evidence)
                .reason(text(risk, "message"))
                .build());
    }

    private String questionForClaim(ClaimEvidenceItemVO claim) {
        ClaimEvidenceStatus status = claim.getStatus();
        String claimText = claim.getClaimText();

        if (status == ClaimEvidenceStatus.RISKY || status == ClaimEvidenceStatus.NO_EVIDENCE) {
            return "报告未找到足够证据支持以下主张：“" + claimText
                    + "”。请说明该主张是否真实实现；如果没有，请明确限定或撤回。";
        }

        if (status == ClaimEvidenceStatus.PARTIAL || status == ClaimEvidenceStatus.DOC_ONLY) {
            return "以下主张目前只有部分或文档证据：“" + claimText
                    + "”。请说明哪些部分已经实现，哪些部分仍缺少代码、配置或运行证据。";
        }

        return "报告认为以下主张已有项目证据支持：“" + claimText
                + "”。请结合具体文件说明它的实现流程、关键边界和你的实际贡献。";
    }

    private int claimPriority(ClaimEvidenceItemVO claim) {
        if (claim.getStatus() == null) {
            return 5;
        }
        return switch (claim.getStatus()) {
            case RISKY -> 0;
            case NO_EVIDENCE -> 1;
            case PARTIAL -> 2;
            case DOC_ONLY -> 3;
            case SUPPORTED -> 4;
        };
    }

    private List<DefenseEvidenceReference> toEvidenceReferences(List<ClaimEvidenceFileVO> evidenceFiles) {
        if (evidenceFiles == null || evidenceFiles.isEmpty()) {
            return List.of();
        }

        return evidenceFiles.stream()
                .filter(Objects::nonNull)
                .filter(evidence -> evidence.getFileId() != null || StringUtils.hasText(evidence.getFilePath()))
                .map(evidence -> DefenseEvidenceReference.builder()
                        .fileId(evidence.getFileId())
                        .filePath(evidence.getFilePath())
                        .evidenceLevel(evidence.getEvidenceLevel())
                        .snippet(evidence.getSnippet())
                        .reason(evidence.getReason())
                        .build())
                .toList();
    }

    private void completeSessionWhenAllQuestionsAnswered(DefenseSession session) {
        List<DefenseQuestion> questions = listQuestions(session.getId());
        if (questions.isEmpty()) {
            return;
        }

        List<Long> questionIds = questions.stream().map(DefenseQuestion::getId).toList();
        Long answeredCount = defenseAnswerMapper.selectCount(
                new LambdaQueryWrapper<DefenseAnswer>()
                        .in(DefenseAnswer::getQuestionId, questionIds)
        );
        if (answeredCount == null || answeredCount < questions.size()) {
            return;
        }

        session.setStatus(STATUS_COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        if (defenseSessionMapper.updateById(session) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 会话完成状态保存失败");
        }
    }

    private List<DefenseQuestionResponse> loadQuestionResponses(DefenseSession session) {
        List<DefenseQuestion> questions = listQuestions(session.getId());
        if (questions.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = questions.stream().map(DefenseQuestion::getId).toList();
        Map<Long, DefenseAnswer> answerMap = defenseAnswerMapper.selectList(
                        new LambdaQueryWrapper<DefenseAnswer>()
                                .in(DefenseAnswer::getQuestionId, questionIds)
                ).stream()
                .collect(Collectors.toMap(
                        DefenseAnswer::getQuestionId,
                        Function.identity(),
                        (left, right) -> left,
                        HashMap::new
                ));

        return questions.stream()
                .map(question -> toQuestionResponse(question, answerMap.get(question.getId())))
                .toList();
    }

    private List<DefenseQuestion> listQuestions(Long sessionId) {
        return defenseQuestionMapper.selectList(
                new LambdaQueryWrapper<DefenseQuestion>()
                        .eq(DefenseQuestion::getSessionId, sessionId)
                        .orderByAsc(DefenseQuestion::getSortOrder)
                        .orderByAsc(DefenseQuestion::getId)
        );
    }

    private DefenseQuestionResponse toQuestionResponse(DefenseQuestion question, DefenseAnswer answer) {
        return DefenseQuestionResponse.builder()
                .id(question.getId())
                .sessionId(question.getSessionId())
                .question(question.getQuestion())
                .category(question.getCategory())
                .relatedClaims(parseClaims(question.getRelatedClaims()))
                .relatedEvidence(parseEvidence(question.getRelatedEvidence()))
                .sortOrder(question.getSortOrder())
                .answer(answer == null ? null : toAnswerResponse(answer))
                .build();
    }

    private DefenseAnswerResponse toAnswerResponse(DefenseAnswer answer) {
        return DefenseAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .answerText(answer.getAnswerText())
                .evaluationStatus(answer.getEvaluationStatus())
                .reviewResult(parseReviewResult(answer))
                .createdAt(answer.getCreatedAt())
                .build();
    }

    private DefenseReviewResult parseReviewResult(DefenseAnswer answer) {
        try {
            return objectMapper.readValue(answer.getReviewResult(), DefenseReviewResult.class);
        } catch (Exception ignored) {
            return DefenseReviewResult.builder()
                    .evidenceAlignment(answer.getEvaluationStatus())
                    .summary("Defense 复盘结果暂时无法解析")
                    .relatedClaims(List.of())
                    .matchedEvidence(List.of())
                    .build();
        }
    }

    private List<String> parseClaims(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> claims = objectMapper.readValue(json, CLAIM_LIST_TYPE);
            return claims == null ? List.of() : claims;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<DefenseEvidenceReference> parseEvidence(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<DefenseEvidenceReference> evidence = objectMapper.readValue(json, EVIDENCE_LIST_TYPE);
            return evidence == null ? List.of() : evidence;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String evaluateAlignment(List<DefenseEvidenceReference> relatedEvidence,
                                     List<DefenseEvidenceReference> matchedEvidence) {
        if (relatedEvidence.isEmpty()) {
            return ALIGNMENT_INSUFFICIENT;
        }
        return matchedEvidence.isEmpty() ? ALIGNMENT_PARTIAL : ALIGNMENT_SUPPORTED;
    }

    private String reviewSummary(String alignment) {
        return switch (alignment) {
            case ALIGNMENT_SUPPORTED -> "回答明确引用了本题绑定的项目证据，判定为证据对齐；该结果不代表能力评分或实现正确性。";
            case ALIGNMENT_PARTIAL -> "本题存在关联证据，但回答未明确引用具体证据文件，当前只能判定为部分对齐。";
            default -> "本题没有可验证的关联证据，无法确认回答与项目证据对齐。";
        };
    }

    private boolean mentionsEvidence(String answerText, DefenseEvidenceReference evidence) {
        if (!StringUtils.hasText(answerText) || evidence == null || !StringUtils.hasText(evidence.getFilePath())) {
            return false;
        }

        String normalizedAnswer = normalizeText(answerText);
        String normalizedPath = normalizeText(evidence.getFilePath());
        if (normalizedPath.length() >= 3 && normalizedAnswer.contains(normalizedPath)) {
            return true;
        }

        int slashIndex = normalizedPath.lastIndexOf('/');
        String fileName = slashIndex < 0 ? normalizedPath : normalizedPath.substring(slashIndex + 1);
        return fileName.length() >= 3 && normalizedAnswer.contains(fileName);
    }

    private String normalizeText(String value) {
        return value == null
                ? ""
                : value.replace('\\', '/').toLowerCase(Locale.ROOT).trim();
    }

    private int countAlignment(List<DefenseAnswerResponse> answers, String alignment) {
        return (int) answers.stream()
                .filter(answer -> alignment.equals(answer.getEvaluationStatus()))
                .count();
    }

    private DefenseSession requireOwnedSession(Long sessionId) {
        DefenseSession session = defenseSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Defense 会话不存在或无权限访问");
        }
        requireOwnedProject(session.getProjectId());
        return session;
    }

    private Project requireOwnedProject(Long projectId) {
        Long userId = getCurrentUserId();
        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限访问 Defense");
        }
        return project;
    }

    private AnalysisReport requireProjectReport(Long projectId, Long reportId) {
        AnalysisReport report = analysisReportMapper.selectById(reportId);
        if (report == null || !projectId.equals(report.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Defense 报告不存在或不属于当前项目");
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

    private String normalizeMode(String mode) {
        String normalizedMode = StringUtils.hasText(mode)
                ? mode.trim().toUpperCase(Locale.ROOT)
                : MODE_EVIDENCE_DEFENSE;
        if (!MODE_EVIDENCE_DEFENSE.equals(normalizedMode)) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "当前 Defense MVP 仅支持 EVIDENCE_DEFENSE 模式"
            );
        }
        return normalizedMode;
    }

    private DefenseSessionResponse toSessionResponse(DefenseSession session) {
        return DefenseSessionResponse.builder()
                .id(session.getId())
                .projectId(session.getProjectId())
                .reportId(session.getReportId())
                .mode(session.getMode())
                .status(session.getStatus())
                .questionCount(session.getQuestionCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Defense 数据序列化失败");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
