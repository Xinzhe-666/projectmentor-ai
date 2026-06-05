package com.xinzhe.projectmentor.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.common.PageResult;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.interview.dto.StartInterviewRequest;
import com.xinzhe.projectmentor.interview.dto.SubmitAnswerRequest;
import com.xinzhe.projectmentor.interview.entity.InterviewMessage;
import com.xinzhe.projectmentor.interview.entity.InterviewSession;
import com.xinzhe.projectmentor.interview.mapper.InterviewMessageMapper;
import com.xinzhe.projectmentor.interview.mapper.InterviewSessionMapper;
import com.xinzhe.projectmentor.interview.vo.InterviewMessageVO;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionListItemVO;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
@Slf4j
public class InterviewService {

    private static final int MAX_QUESTION_COUNT = 8;

    private static final String SKIP_ANSWER_PREFIX = "[PM_INTERVIEW_SKIP]";

    private static final String META_START = "[PM_INTERVIEW_META]";

    private static final String META_END = "[/PM_INTERVIEW_META]";

    private final InterviewSessionMapper interviewSessionMapper;

    private final InterviewMessageMapper interviewMessageMapper;

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

    private final LlmClient llmClient;

    private final InterviewPromptBuilder interviewPromptBuilder;

    private final AiJsonUtil aiJsonUtil;

    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO startInterview(StartInterviewRequest request) {
        Long userId = getCurrentUserId();
        Project project = checkProjectOwner(request.getProjectId(), userId);

        String mode = normalizeMode(request.getMode());

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setProjectId(project.getId());
        session.setMode(mode);
        session.setStatus("RUNNING");

        interviewSessionMapper.insert(session);

        List<ProjectFile> projectFiles = listProjectFiles(project.getId());
        QuestionMeta ruleFirstMeta = buildQuestionMeta(projectFiles, categoryForQuestion(1), "第一题用于确认项目真实性、职责边界和可解释范围。");
        String ruleFirstQuestion = buildFirstQuestion(project, mode, ruleFirstMeta);
        QuestionDraft firstQuestion = generateAiFirstQuestion(project, mode, projectFiles, ruleFirstQuestion, ruleFirstMeta);

        InterviewMessage message = new InterviewMessage();
        message.setSessionId(session.getId());
        message.setRole("INTERVIEWER");
        message.setContent(withQuestionMeta(firstQuestion.question(), firstQuestion.meta(), 1));

        interviewMessageMapper.insert(message);

        return getSessionDetail(session.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        Long userId = getCurrentUserId();
        InterviewSession session = checkSessionOwner(sessionId, userId);

        if ("FINISHED".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "面试已结束，不能继续回答");
        }

        Project project = projectMapper.selectById(session.getProjectId());
        List<ProjectFile> projectFiles = listProjectFiles(session.getProjectId());
        List<InterviewMessage> history = listSessionMessages(sessionId);
        int currentQuestionCount = countInterviewerQuestions(history);
        boolean skipped = isSkippedAnswer(request.getAnswer());
        String answer = skipped ? "已跳过本题" : request.getAnswer();

        InterviewMessage userMessage = new InterviewMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        userMessage.setContent(skipped ? SKIP_ANSWER_PREFIX + " 已跳过本题" : answer);

        String mode = normalizeMode(session.getMode());
        QuestionMeta followUpMeta = buildQuestionMeta(
                projectFiles,
                categoryForQuestion(currentQuestionCount + 1),
                skipped ? "用户跳过上一题，下一题降低假设强度并继续确认可解释证据。" : "基于用户回答继续追问，但保持证据约束。"
        );
        AnswerEvaluation ruleEvaluation = skipped
                ? new AnswerEvaluation(0, "已跳过本题，本题不计入有效回答。", null, followUpMeta)
                : evaluateAnswer(answer);
        String ruleFollowUpQuestion = buildFollowUpQuestion(project, mode, answer, ruleEvaluation, followUpMeta);
        AnswerEvaluation evaluation = skipped
                ? new AnswerEvaluation(0, "已跳过本题，本题不计入有效回答。", ruleFollowUpQuestion, followUpMeta)
                : enhanceEvaluationWithAi(
                        project,
                        mode,
                        answer,
                        ruleEvaluation,
                        ruleFollowUpQuestion,
                        projectFiles,
                        history,
                        followUpMeta
                );

        userMessage.setScore(evaluation.score());
        userMessage.setFeedback(evaluation.feedback());

        interviewMessageMapper.insert(userMessage);

        if (currentQuestionCount >= MAX_QUESTION_COUNT) {
            return finishSession(session, "已达到本轮面试的核心问题数量上限。");
        }

        String followUpQuestion = isBlank(evaluation.followUpQuestion())
                ? ruleFollowUpQuestion
                : evaluation.followUpQuestion();
        QuestionMeta nextQuestionMeta = evaluation.questionMeta() == null ? followUpMeta : evaluation.questionMeta();

        InterviewMessage interviewerMessage = new InterviewMessage();
        interviewerMessage.setSessionId(sessionId);
        interviewerMessage.setRole("INTERVIEWER");
        interviewerMessage.setContent(withQuestionMeta(followUpQuestion, nextQuestionMeta, currentQuestionCount + 1));

        interviewMessageMapper.insert(interviewerMessage);

        return getSessionDetail(sessionId);
    }

    public InterviewSessionVO getSessionDetail(Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSession session = checkSessionOwner(sessionId, userId);
        Project project = projectMapper.selectById(session.getProjectId());

        List<InterviewMessage> messages = listSessionMessages(sessionId);

        return InterviewSessionVO.builder()
                .id(session.getId())
                .projectId(session.getProjectId())
                .projectName(project == null ? null : project.getName())
                .mode(normalizeMode(session.getMode()))
                .status(session.getStatus())
                .totalScore(session.getTotalScore())
                .summary(session.getSummary())
                .createTime(session.getCreateTime())
                .finishTime(session.getFinishTime())
                .messages(messages.stream().map(this::toMessageVO).toList())
                .build();
    }

    public PageResult<InterviewSessionListItemVO> listMySessions(Integer page,
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
            return emptySessionPage(safePage, safeSize);
        }

        Set<Long> ownedProjectIds = ownedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> keywordProjectIds = matchProjectIdsByKeyword(ownedProjects, keyword);

        Long total = interviewSessionMapper.selectCount(buildMySessionWrapper(userId, ownedProjectIds, keyword, keywordProjectIds));
        if (total == null || total == 0) {
            return emptySessionPage(safePage, safeSize);
        }

        int offset = (safePage - 1) * safeSize;
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
                buildMySessionWrapper(userId, ownedProjectIds, keyword, keywordProjectIds)
                        .orderByDesc(InterviewSession::getCreateTime)
                        .orderByDesc(InterviewSession::getId)
                        .last("LIMIT " + offset + ", " + safeSize)
        );

        Map<Long, Project> projectMap = ownedProjects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left, HashMap::new));
        Map<Long, List<InterviewMessage>> messageMap = loadSessionMessageMap(sessions.stream()
                .map(InterviewSession::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return PageResult.<InterviewSessionListItemVO>builder()
                .records(sessions.stream()
                        .map(session -> toListItemVO(session, projectMap, messageMap))
                        .toList())
                .total(total)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public List<InterviewSessionListItemVO> listRecentMySessions(Integer limit) {
        return listMySessions(1, sanitizeLimit(limit), null, null).getRecords();
    }

    public Long countMySessions() {
        Long userId = getCurrentUserId();

        return interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO finishInterview(Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSession session = checkSessionOwner(sessionId, userId);

        if ("FINISHED".equalsIgnoreCase(session.getStatus())) {
            return getSessionDetail(sessionId);
        }

        return finishSession(session, "用户主动结束面试。");
    }

    private InterviewSessionVO finishSession(InterviewSession session, String finishReason) {
        Long sessionId = session.getId();
        List<InterviewMessage> userMessages = interviewMessageMapper.selectList(
                new LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId)
                        .eq(InterviewMessage::getRole, "USER")
        );
        List<InterviewMessage> interviewerMessages = interviewMessageMapper.selectList(
                new LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId)
                        .eq(InterviewMessage::getRole, "INTERVIEWER")
        );

        int totalScore = calculateTotalScore(userMessages);
        int skippedCount = countSkippedAnswers(userMessages);
        int answeredCount = Math.max(0, userMessages.size() - skippedCount);
        int unansweredCount = Math.max(0, interviewerMessages.size() - userMessages.size());
        String summary = buildSummary(totalScore, answeredCount, skippedCount, unansweredCount, finishReason);

        session.setStatus("FINISHED");
        session.setTotalScore(totalScore);
        session.setSummary(summary);
        session.setFinishTime(LocalDateTime.now());

        interviewSessionMapper.updateById(session);

        InterviewMessage systemMessage = new InterviewMessage();
        systemMessage.setSessionId(sessionId);
        systemMessage.setRole("SYSTEM");
        systemMessage.setContent(summary);
        systemMessage.setScore(totalScore);
        systemMessage.setFeedback("本次模拟面试已结束。");

        interviewMessageMapper.insert(systemMessage);

        return getSessionDetail(sessionId);
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

    private LambdaQueryWrapper<InterviewSession> buildMySessionWrapper(Long userId,
                                                                       Set<Long> projectIds,
                                                                       String keyword,
                                                                       Set<Long> keywordProjectIds) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .in(InterviewSession::getProjectId, projectIds);

        if (!StringUtils.hasText(keyword)) {
            return wrapper;
        }

        String normalizedKeyword = keyword.trim();
        wrapper.and(query -> {
            query.like(InterviewSession::getSummary, normalizedKeyword)
                    .or()
                    .like(InterviewSession::getStatus, normalizedKeyword)
                    .or()
                    .like(InterviewSession::getMode, normalizedKeyword);
            if (keywordProjectIds != null && !keywordProjectIds.isEmpty()) {
                query.or().in(InterviewSession::getProjectId, keywordProjectIds);
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

    private Map<Long, List<InterviewMessage>> loadSessionMessageMap(Set<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return interviewMessageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                        .select(InterviewMessage::getId, InterviewMessage::getSessionId,
                                InterviewMessage::getRole, InterviewMessage::getContent)
                        .in(InterviewMessage::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.groupingBy(InterviewMessage::getSessionId));
    }

    private InterviewSessionListItemVO toListItemVO(InterviewSession session,
                                                    Map<Long, Project> projectMap,
                                                    Map<Long, List<InterviewMessage>> messageMap) {
        List<InterviewMessage> messages = messageMap.getOrDefault(session.getId(), Collections.emptyList());
        int questionCount = (int) messages.stream()
                .filter(message -> "INTERVIEWER".equalsIgnoreCase(message.getRole()))
                .count();
        int skippedCount = (int) messages.stream()
                .filter(message -> "USER".equalsIgnoreCase(message.getRole()))
                .filter(message -> isSkippedAnswer(message.getContent()))
                .count();
        int answeredCount = (int) messages.stream()
                .filter(message -> "USER".equalsIgnoreCase(message.getRole()))
                .filter(message -> !isSkippedAnswer(message.getContent()))
                .count();
        Project project = projectMap.get(session.getProjectId());

        return InterviewSessionListItemVO.builder()
                .sessionId(session.getId())
                .projectId(session.getProjectId())
                .projectName(project == null ? null : project.getName())
                .totalScore(session.getTotalScore())
                .questionCount(questionCount)
                .answeredCount(answeredCount)
                .skippedCount(skippedCount)
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getFinishTime() == null ? session.getCreateTime() : session.getFinishTime())
                .build();
    }

    private PageResult<InterviewSessionListItemVO> emptySessionPage(int page, int size) {
        return PageResult.<InterviewSessionListItemVO>builder()
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

    private Project checkProjectOwner(Long projectId, Long userId) {
        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限面试");
        }

        return project;
    }

    private InterviewSession checkSessionOwner(Long sessionId, Long userId) {
        InterviewSession session = interviewSessionMapper.selectOne(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getId, sessionId)
                        .eq(InterviewSession::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "面试会话不存在或无权限访问");
        }

        return session;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "TECH_DEEP_DIVE";
        }

        String normalizedMode = mode.toUpperCase(Locale.ROOT);
        if ("HUAWEI_BACKEND".equals(normalizedMode)) {
            return "JAVA_BACKEND";
        }

        return normalizedMode;
    }

    private String buildFirstQuestion(Project project, String mode, QuestionMeta meta) {
        if ("NONE".equals(meta.evidenceStrength())) {
            return "当前上传材料中还没有可用项目文件证据。请你先介绍项目“" + project.getName() + "”的真实完成范围、你负责的部分，以及后续能补充哪些代码或配置证据。";
        }

        if ("WEAK".equals(meta.evidenceStrength())) {
            return "README 中有项目描述，但当前代码证据不足。请你先介绍项目“" + project.getName() + "”中你实际完成的部分，并说明哪些内容有代码或配置可以支撑。";
        }

        if (!"STRONG".equals(meta.evidenceStrength())
                && ("JAVA_BACKEND".equals(mode) || "AI_PROJECT".equals(mode))) {
            return "当前证据更多来自配置、文档或部署文件，代码证据还不充分。请你说明项目“" + project.getName() + "”中哪些能力是实际实现，哪些只是配置或 README 描述。";
        }

        return switch (mode) {
            case "HR_REALITY" -> "请你先用 1 分钟介绍一下项目“" + project.getName() + "”。重点说明：这个项目是不是你独立完成的？你具体负责了哪些部分？AI 在里面帮了你什么？";
            case "PRESSURE" -> "我看你的项目 README 里可能有一些包装化表述。请你证明一下：这个项目哪些功能是真实实现的？哪些只是计划或描述？";
            case "JAVA_BACKEND" -> "如果你作为 Java 后端实习生接手一个类似项目，你会如何快速看懂它的 Controller、Service、Mapper 和数据库表结构？";
            case "AI_PROJECT" -> "你这个项目里哪些地方真正体现了 AI 能力？只是调用大模型 API，还是有规则扫描、证据链或 RAG 等真实逻辑？";
            default -> "请你介绍一下项目“" + project.getName() + "”的核心业务流程、技术栈选择，以及你认为最能体现后端能力的部分。";
        };
    }

    private AnswerEvaluation evaluateAnswer(String answer) {
        int score = 60;
        String lower = answer.toLowerCase(Locale.ROOT);

        if (answer.length() >= 80) {
            score += 10;
        }

        if (containsAny(lower, List.of("controller", "service", "mapper", "数据库", "表", "jwt", "redis", "规则扫描", "证据链"))) {
            score += 10;
        }

        if (containsAny(lower, List.of("我负责", "我实现", "我设计", "我遇到", "我解决"))) {
            score += 8;
        }

        if (containsAny(lower, List.of("不清楚", "不知道", "大概", "可能", "反正"))) {
            score -= 10;
        }

        score = clamp(score);

        String feedback;
        if (score >= 80) {
            feedback = "回答较具体，能体现一定项目理解。建议继续补充关键文件、接口流程和数据表设计细节。";
        } else if (score >= 60) {
            feedback = "回答基本可用，但还偏概括。建议增加具体实现，例如涉及哪些类、哪些接口、哪些表、如何流转。";
        } else {
            feedback = "回答偏空泛，抗追问能力较弱。建议用“背景-职责-实现-难点-结果”的结构重新组织。";
        }

        return new AnswerEvaluation(score, feedback, null, null);
    }

    private String buildFollowUpQuestion(Project project, String mode, String answer, AnswerEvaluation evaluation, QuestionMeta meta) {
        String lower = answer.toLowerCase(Locale.ROOT);
        boolean hasStrongEvidence = "STRONG".equals(meta.evidenceStrength()) || "MEDIUM".equals(meta.evidenceStrength());

        if (containsAny(lower, List.of("jwt", "token", "登录"))) {
            if (!hasStrongEvidence) {
                return "你刚才提到了登录或 token。当前证据不足，请说明你是否实际实现了这部分；如果实现了，请指出可以支撑它的文件、接口流程或配置。";
            }
            return "你刚才提到了 JWT。请具体说一下：用户登录成功后 token 是怎么生成的？后端后续请求如何从 token 里拿到 userId？";
        }

        if (containsAny(lower, List.of("redis", "缓存"))) {
            if (!hasStrongEvidence) {
                return "你刚才提到了缓存。当前证据不足，请说明项目里是否实际接入了缓存；如果只是 README 描述，请区分计划、配置和真实代码实现。";
            }
            return "你刚才提到了 Redis。请说明这个项目里 Redis 具体缓存了什么？key 怎么设计？过期时间怎么设置？";
        }

        if (containsAny(lower, List.of("规则扫描", "证据链", "readme"))) {
            return "你刚才提到了规则扫描或证据链。请解释一下：系统为什么不能只相信 README？它如何判断 README 中的技术表述是否有证据支撑？";
        }

        if (evaluation.score() < 65) {
            return "你的回答还比较泛。请你结合当前项目中真实存在的文件或配置，再说明一次你真正实现了哪些功能；证据不足的部分请明确说不建议写成核心实现。";
        }

        return "如果面试官质疑这个项目是 AI 帮你生成的，你会如何证明自己理解了项目核心代码和设计思路？";
    }

    private QuestionDraft generateAiFirstQuestion(Project project,
                                                  String mode,
                                                  List<ProjectFile> projectFiles,
                                                  String ruleFirstQuestion,
                                                  QuestionMeta ruleFirstMeta) {
        try {
            String content = llmClient.chat(
                    "INTERVIEW",
                    interviewPromptBuilder.buildSystemPrompt(),
                    interviewPromptBuilder.buildFirstQuestionPrompt(project, mode, projectFiles)
            );

            JsonNode root = aiJsonUtil.safeReadTree(content);
            String question = aiJsonUtil.getText(root, "question");
            if (isBlank(question)) {
                question = stripCodeFence(content);
            }

            QuestionMeta meta = parseQuestionMeta(root, projectFiles, ruleFirstMeta, "category");
            if (shouldUseRuleQuestion(question, meta, projectFiles)) {
                return new QuestionDraft(ruleFirstQuestion, ruleFirstMeta);
            }

            return new QuestionDraft(normalizeQuestion(question, ruleFirstQuestion), meta);
        } catch (Exception e) {
            log.info("AI first interview question unavailable, fallback to rule question: {}", e.getMessage());
            return new QuestionDraft(ruleFirstQuestion, ruleFirstMeta);
        }
    }

    private AnswerEvaluation enhanceEvaluationWithAi(Project project,
                                                     String mode,
                                                     String answer,
                                                     AnswerEvaluation ruleEvaluation,
                                                     String ruleFollowUpQuestion,
                                                     List<ProjectFile> projectFiles,
                                                     List<InterviewMessage> history,
                                                     QuestionMeta ruleFollowUpMeta) {
        try {
            String content = llmClient.chat(
                    "INTERVIEW",
                    interviewPromptBuilder.buildSystemPrompt(),
                    interviewPromptBuilder.buildEvaluationPrompt(
                            project,
                            mode,
                            answer,
                            ruleEvaluation.score(),
                            ruleEvaluation.feedback(),
                            ruleFollowUpQuestion,
                            projectFiles,
                            history
                    )
            );

            AnswerEvaluation aiEvaluation = parseAiEvaluation(content, ruleFollowUpQuestion, projectFiles, ruleFollowUpMeta);
            return aiEvaluation == null ? new AnswerEvaluation(
                    ruleEvaluation.score(),
                    ruleEvaluation.feedback(),
                    ruleFollowUpQuestion,
                    ruleFollowUpMeta
            ) : aiEvaluation;
        } catch (Exception e) {
            log.info("AI interview evaluation unavailable, fallback to rule evaluation: {}", e.getMessage());
            return new AnswerEvaluation(ruleEvaluation.score(), ruleEvaluation.feedback(), ruleFollowUpQuestion, ruleFollowUpMeta);
        }
    }

    private AnswerEvaluation parseAiEvaluation(String content,
                                               String ruleFollowUpQuestion,
                                               List<ProjectFile> projectFiles,
                                               QuestionMeta ruleFollowUpMeta) {
        JsonNode root = aiJsonUtil.safeReadTree(content);
        if (root == null || !root.isObject()) {
            return null;
        }

        Integer score = getScore(root, "score");
        String feedback = aiJsonUtil.getText(root, "feedback");
        String followUpQuestion = normalizeQuestion(
                aiJsonUtil.getText(root, "followUpQuestion"),
                ruleFollowUpQuestion
        );
        QuestionMeta questionMeta = parseQuestionMeta(root, projectFiles, ruleFollowUpMeta, "followUpCategory");

        if (score == null
                || isBlank(feedback)
                || isBlank(followUpQuestion)
                || shouldUseRuleQuestion(followUpQuestion, questionMeta, projectFiles)) {
            return null;
        }

        return new AnswerEvaluation(score, feedback, followUpQuestion, questionMeta);
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

    private String normalizeQuestion(String question, String fallback) {
        if (isBlank(question)) {
            return fallback;
        }

        String normalized = question.trim();
        if (normalized.length() > 600) {
            return fallback;
        }

        return normalized;
    }

    private String stripCodeFence(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    private int calculateTotalScore(List<InterviewMessage> userMessages) {
        if (userMessages == null || userMessages.isEmpty()) {
            return 0;
        }

        List<InterviewMessage> answeredMessages = userMessages.stream()
                .filter(message -> !isSkippedAnswer(message.getContent()))
                .toList();

        if (answeredMessages.isEmpty()) {
            return 0;
        }

        int sum = answeredMessages.stream()
                .map(InterviewMessage::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .sum();

        return Math.round((float) sum / answeredMessages.size());
    }

    private String buildSummary(int totalScore,
                                int answeredCount,
                                int skippedCount,
                                int unansweredCount,
                                String finishReason) {
        String mainIssue;
        String suggestion;

        if (answeredCount == 0) {
            mainIssue = "本次面试没有有效回答，暂时无法判断项目表达质量。";
            suggestion = "建议先梳理 README、关键代码文件和真实职责，再重新进行一轮 6 到 8 题的核心追问。";
        } else if (totalScore >= 80) {
            mainIssue = "整体表达较具体，能体现一定项目理解；后续重点是把回答继续绑定到真实文件和证据。";
            suggestion = "继续补充关键接口、配置、数据流和可追问边界，避免把证据不足的内容写成核心实现。";
        } else if (totalScore >= 60) {
            mainIssue = "能够说明项目基本情况，但部分回答仍偏概括，抗追问时需要更明确的文件和实现依据。";
            suggestion = "按“功能-文件-流程-边界”的顺序重写回答，并标出哪些能力只有 README 或配置证据。";
        } else {
            mainIssue = "回答偏空泛或证据绑定不足，面试中容易被继续追问实现细节。";
            suggestion = "优先复习项目核心流程、关键代码和证据链设计；证据不足的能力不建议写成核心实现。";
        }

        return """
                本次模拟面试已结束。%s

                - 总分：%s
                - 已回答：%s
                - 已跳过：%s
                - 未回答：%s

                主要问题：
                %s

                改进建议：
                %s
                """.formatted(
                isBlank(finishReason) ? "" : finishReason,
                totalScore,
                answeredCount,
                skippedCount,
                unansweredCount,
                mainIssue,
                suggestion
        ).trim();
    }

    private int countInterviewerQuestions(List<InterviewMessage> messages) {
        if (messages == null) {
            return 0;
        }

        return (int) messages.stream()
                .filter(message -> "INTERVIEWER".equalsIgnoreCase(message.getRole()))
                .count();
    }

    private int countSkippedAnswers(List<InterviewMessage> userMessages) {
        if (userMessages == null) {
            return 0;
        }

        return (int) userMessages.stream()
                .filter(message -> isSkippedAnswer(message.getContent()))
                .count();
    }

    private boolean isSkippedAnswer(String answer) {
        return answer != null && answer.trim().startsWith(SKIP_ANSWER_PREFIX);
    }

    private String categoryForQuestion(int questionIndex) {
        return switch (questionIndex) {
            case 1 -> "PROJECT_REALITY";
            case 2, 6 -> "TECH_IMPLEMENTATION";
            case 3, 7 -> "EVIDENCE_EXPLANATION";
            case 4 -> "RESUME_RISK";
            default -> "PRESSURE_FOLLOW_UP";
        };
    }

    private String normalizeCategory(String category, String fallback) {
        if (category == null || category.isBlank()) {
            return fallback;
        }

        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (List.of(
                "PROJECT_REALITY",
                "TECH_IMPLEMENTATION",
                "EVIDENCE_EXPLANATION",
                "RESUME_RISK",
                "PRESSURE_FOLLOW_UP"
        ).contains(normalized)) {
            return normalized;
        }

        return fallback;
    }

    private String normalizeEvidenceStrength(String evidenceStrength, String fallback) {
        if (evidenceStrength == null || evidenceStrength.isBlank()) {
            return fallback;
        }

        String normalized = evidenceStrength.trim().toUpperCase(Locale.ROOT);
        if (List.of("STRONG", "MEDIUM", "WEAK", "NONE").contains(normalized)) {
            return normalized;
        }

        return fallback;
    }

    private QuestionMeta buildQuestionMeta(List<ProjectFile> projectFiles, String category, String reason) {
        ProjectFile file = selectEvidenceFile(projectFiles, category);
        if (file == null) {
            return new QuestionMeta(category, "NONE", "", reason + " 当前没有可绑定的项目文件证据。", null);
        }

        String sourceFile = file.getFilePath();
        String strength = inferEvidenceStrength(file);
        String detailReason = reason + " 关联文件：" + sourceFile + "。";
        return new QuestionMeta(category, strength, sourceFile, detailReason, null);
    }

    private ProjectFile selectEvidenceFile(List<ProjectFile> files, String category) {
        if (files == null || files.isEmpty()) {
            return null;
        }

        if ("TECH_IMPLEMENTATION".equals(category)) {
            ProjectFile codeFile = firstMatchingFile(files, this::isCodeEvidence);
            if (codeFile != null) {
                return codeFile;
            }
        }

        if ("EVIDENCE_EXPLANATION".equals(category) || "PROJECT_REALITY".equals(category)) {
            ProjectFile configFile = firstMatchingFile(files, this::isConfigEvidence);
            if (configFile != null) {
                return configFile;
            }
        }

        if ("RESUME_RISK".equals(category)) {
            ProjectFile readmeFile = firstMatchingFile(files, this::isDocEvidence);
            if (readmeFile != null) {
                return readmeFile;
            }
        }

        ProjectFile codeFile = firstMatchingFile(files, this::isCodeEvidence);
        if (codeFile != null) {
            return codeFile;
        }

        ProjectFile configFile = firstMatchingFile(files, this::isConfigEvidence);
        if (configFile != null) {
            return configFile;
        }

        ProjectFile docFile = firstMatchingFile(files, this::isDocEvidence);
        return docFile == null ? files.get(0) : docFile;
    }

    private ProjectFile firstMatchingFile(List<ProjectFile> files, FilePredicate predicate) {
        for (ProjectFile file : files) {
            if (predicate.matches(file)) {
                return file;
            }
        }
        return null;
    }

    private boolean isCodeEvidence(ProjectFile file) {
        String type = safeUpper(file.getFileType());
        String path = safeLower(file.getFilePath());
        return path.endsWith(".java")
                || List.of("CONTROLLER", "SERVICE", "MAPPER", "ENTITY", "UTIL").contains(type);
    }

    private boolean isConfigEvidence(ProjectFile file) {
        String type = safeUpper(file.getFileType());
        String path = safeLower(file.getFilePath());
        return List.of("CONFIG", "POM", "PACKAGE", "DOCKER", "DOCKER_COMPOSE", "SQL", "GITIGNORE").contains(type)
                || path.endsWith(".yml")
                || path.endsWith(".yaml")
                || path.endsWith(".properties")
                || path.endsWith(".xml")
                || path.endsWith(".sql")
                || path.endsWith(".json")
                || path.endsWith("dockerfile");
    }

    private boolean isDocEvidence(ProjectFile file) {
        String type = safeUpper(file.getFileType());
        String path = safeLower(file.getFilePath());
        return "README".equals(type) || path.endsWith(".md");
    }

    private String inferEvidenceStrength(ProjectFile file) {
        if (file == null) {
            return "NONE";
        }

        if (isCodeEvidence(file)) {
            return "STRONG";
        }

        if (isConfigEvidence(file)) {
            return "MEDIUM";
        }

        if (isDocEvidence(file)) {
            return "WEAK";
        }

        return "WEAK";
    }

    private QuestionMeta parseQuestionMeta(JsonNode root,
                                           List<ProjectFile> projectFiles,
                                           QuestionMeta fallback,
                                           String categoryField) {
        if (root == null || !root.isObject()) {
            return fallback;
        }

        String aiSourceFile = aiJsonUtil.getText(root, "sourceFile");
        String normalizedSourceFile = normalizeSourceFile(aiSourceFile, projectFiles);
        if (!isBlank(aiSourceFile) && isBlank(normalizedSourceFile)) {
            return fallback;
        }

        String category = normalizeCategory(aiJsonUtil.getText(root, categoryField), fallback.category());
        String evidenceStrength = normalizeEvidenceStrength(aiJsonUtil.getText(root, "evidenceStrength"), fallback.evidenceStrength());
        String sourceFile = isBlank(normalizedSourceFile) ? fallback.sourceFile() : normalizedSourceFile;
        String reason = aiJsonUtil.getText(root, "reason");
        if (isBlank(reason)) {
            reason = fallback.reason();
        }

        return new QuestionMeta(category, evidenceStrength, sourceFile, reason, null);
    }

    private boolean shouldUseRuleQuestion(String question, QuestionMeta meta, List<ProjectFile> projectFiles) {
        return isBlank(question)
                || question.trim().length() > 600
                || referencesUnknownFile(question, projectFiles)
                || (meta != null && !isBlank(meta.sourceFile()) && !sourceFileExists(meta.sourceFile(), projectFiles));
    }

    private boolean referencesUnknownFile(String question, List<ProjectFile> projectFiles) {
        String lower = safeLower(question);
        boolean hasFileLikeText = lower.matches("(?s).*(\\.java|\\.xml|\\.ya?ml|\\.properties|\\.sql|\\.json|\\.md|dockerfile|docker-compose).*");
        if (!hasFileLikeText) {
            return false;
        }

        if (projectFiles == null || projectFiles.isEmpty()) {
            return true;
        }

        for (ProjectFile file : projectFiles) {
            String path = safeLower(file.getFilePath());
            String fileName = fileName(path);
            if (!path.isBlank() && (lower.contains(path) || lower.contains(fileName))) {
                return false;
            }
        }

        return true;
    }

    private String normalizeSourceFile(String sourceFile, List<ProjectFile> projectFiles) {
        if (isBlank(sourceFile) || projectFiles == null) {
            return "";
        }

        String lowerSource = safeLower(sourceFile);
        for (ProjectFile file : projectFiles) {
            String path = safeLower(file.getFilePath());
            if (path.equals(lowerSource) || fileName(path).equals(lowerSource)) {
                return file.getFilePath();
            }
        }

        return "";
    }

    private boolean sourceFileExists(String sourceFile, List<ProjectFile> projectFiles) {
        return !isBlank(normalizeSourceFile(sourceFile, projectFiles));
    }

    private String withQuestionMeta(String question, QuestionMeta meta, int questionIndex) {
        QuestionMeta safeMeta = meta == null
                ? new QuestionMeta(categoryForQuestion(questionIndex), "NONE", "", "未找到可绑定证据。", null)
                : meta;

        return META_START + "\n"
                + "questionIndex=" + questionIndex + "\n"
                + "category=" + sanitizeMetaValue(safeMeta.category()) + "\n"
                + "evidenceStrength=" + sanitizeMetaValue(safeMeta.evidenceStrength()) + "\n"
                + "sourceFile=" + sanitizeMetaValue(safeMeta.sourceFile()) + "\n"
                + "reason=" + sanitizeMetaValue(safeMeta.reason()) + "\n"
                + META_END + "\n"
                + question;
    }

    private ParsedMessageContent parseMessageContent(String content) {
        if (content == null || !content.startsWith(META_START)) {
            return new ParsedMessageContent(content, null);
        }

        int metaEndIndex = content.indexOf(META_END);
        if (metaEndIndex < 0) {
            return new ParsedMessageContent(content, null);
        }

        String metaBlock = content.substring(META_START.length(), metaEndIndex).trim();
        String visibleContent = content.substring(metaEndIndex + META_END.length()).trim();
        Map<String, String> metaMap = new HashMap<>();
        for (String line : metaBlock.split("\\R")) {
            int splitIndex = line.indexOf('=');
            if (splitIndex <= 0) {
                continue;
            }
            metaMap.put(line.substring(0, splitIndex).trim(), line.substring(splitIndex + 1).trim());
        }

        Integer questionIndex = null;
        try {
            questionIndex = Integer.parseInt(metaMap.getOrDefault("questionIndex", ""));
        } catch (NumberFormatException ignored) {
            questionIndex = null;
        }

        QuestionMeta meta = new QuestionMeta(
                metaMap.getOrDefault("category", ""),
                metaMap.getOrDefault("evidenceStrength", ""),
                metaMap.getOrDefault("sourceFile", ""),
                metaMap.getOrDefault("reason", ""),
                questionIndex
        );

        return new ParsedMessageContent(visibleContent, meta);
    }

    private String sanitizeMetaValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String fileName(String path) {
        if (path == null) {
            return "";
        }
        int slashIndex = path.lastIndexOf('/');
        return slashIndex < 0 ? path : path.substring(slashIndex + 1);
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<ProjectFile> listProjectFiles(Long projectId) {
        return projectFileMapper.selectList(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
                        .orderByAsc(ProjectFile::getFilePath)
        );
    }

    private List<InterviewMessage> listSessionMessages(Long sessionId) {
        return interviewMessageMapper.selectList(
                new LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId)
                        .orderByAsc(InterviewMessage::getCreateTime)
                        .orderByAsc(InterviewMessage::getId)
        );
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private InterviewMessageVO toMessageVO(InterviewMessage message) {
        ParsedMessageContent parsedContent = parseMessageContent(message.getContent());
        QuestionMeta meta = parsedContent.meta();
        boolean skipped = isSkippedAnswer(message.getContent());

        return InterviewMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(skipped ? "已跳过本题" : parsedContent.content())
                .score(message.getScore())
                .feedback(message.getFeedback())
                .questionCategory(meta == null ? null : meta.category())
                .evidenceStrength(meta == null ? null : meta.evidenceStrength())
                .sourceFile(meta == null ? null : meta.sourceFile())
                .reason(meta == null ? null : meta.reason())
                .questionIndex(meta == null ? null : meta.questionIndex())
                .skipped(skipped)
                .createTime(message.getCreateTime())
                .build();
    }

    private record AnswerEvaluation(int score, String feedback, String followUpQuestion, QuestionMeta questionMeta) {
    }

    private record QuestionDraft(String question, QuestionMeta meta) {
    }

    private record QuestionMeta(String category, String evidenceStrength, String sourceFile, String reason, Integer questionIndex) {
    }

    private record ParsedMessageContent(String content, QuestionMeta meta) {
    }

    @FunctionalInterface
    private interface FilePredicate {
        boolean matches(ProjectFile file);
    }
}
