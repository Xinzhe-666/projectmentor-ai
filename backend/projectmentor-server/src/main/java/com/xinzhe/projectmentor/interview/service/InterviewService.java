package com.xinzhe.projectmentor.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.interview.dto.StartInterviewRequest;
import com.xinzhe.projectmentor.interview.dto.SubmitAnswerRequest;
import com.xinzhe.projectmentor.interview.entity.InterviewMessage;
import com.xinzhe.projectmentor.interview.entity.InterviewSession;
import com.xinzhe.projectmentor.interview.mapper.InterviewMessageMapper;
import com.xinzhe.projectmentor.interview.mapper.InterviewSessionMapper;
import com.xinzhe.projectmentor.interview.vo.InterviewMessageVO;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionMapper interviewSessionMapper;

    private final InterviewMessageMapper interviewMessageMapper;

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

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

        String firstQuestion = buildFirstQuestion(project, mode);

        InterviewMessage message = new InterviewMessage();
        message.setSessionId(session.getId());
        message.setRole("INTERVIEWER");
        message.setContent(firstQuestion);

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

        InterviewMessage userMessage = new InterviewMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        userMessage.setContent(request.getAnswer());

        AnswerEvaluation evaluation = evaluateAnswer(request.getAnswer());

        userMessage.setScore(evaluation.score());
        userMessage.setFeedback(evaluation.feedback());

        interviewMessageMapper.insert(userMessage);

        String followUpQuestion = buildFollowUpQuestion(project, session.getMode(), request.getAnswer(), evaluation);

        InterviewMessage interviewerMessage = new InterviewMessage();
        interviewerMessage.setSessionId(sessionId);
        interviewerMessage.setRole("INTERVIEWER");
        interviewerMessage.setContent(followUpQuestion);

        interviewMessageMapper.insert(interviewerMessage);

        return getSessionDetail(sessionId);
    }

    public InterviewSessionVO getSessionDetail(Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSession session = checkSessionOwner(sessionId, userId);
        Project project = projectMapper.selectById(session.getProjectId());

        List<InterviewMessage> messages = interviewMessageMapper.selectList(
                new LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId)
                        .orderByAsc(InterviewMessage::getCreateTime)
                        .orderByAsc(InterviewMessage::getId)
        );

        return InterviewSessionVO.builder()
                .id(session.getId())
                .projectId(session.getProjectId())
                .projectName(project == null ? null : project.getName())
                .mode(session.getMode())
                .status(session.getStatus())
                .totalScore(session.getTotalScore())
                .summary(session.getSummary())
                .createTime(session.getCreateTime())
                .finishTime(session.getFinishTime())
                .messages(messages.stream().map(this::toMessageVO).toList())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO finishInterview(Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSession session = checkSessionOwner(sessionId, userId);

        List<InterviewMessage> userMessages = interviewMessageMapper.selectList(
                new LambdaQueryWrapper<InterviewMessage>()
                        .eq(InterviewMessage::getSessionId, sessionId)
                        .eq(InterviewMessage::getRole, "USER")
        );

        int totalScore = calculateTotalScore(userMessages);
        String summary = buildSummary(totalScore, userMessages.size());

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

        return mode.toUpperCase(Locale.ROOT);
    }

    private String buildFirstQuestion(Project project, String mode) {
        return switch (mode) {
            case "HR_REALITY" -> "请你先用 1 分钟介绍一下项目“" + project.getName() + "”。重点说明：这个项目是不是你独立完成的？你具体负责了哪些部分？AI 在里面帮了你什么？";
            case "PRESSURE" -> "我看你的项目 README 里可能有一些包装化表述。请你证明一下：这个项目哪些功能是真实实现的？哪些只是计划或描述？";
            case "HUAWEI_BACKEND" -> "如果你在华为后端实习中接手一个类似项目，你会如何快速看懂它的 Controller、Service、Mapper 和数据库表结构？";
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

        return new AnswerEvaluation(score, feedback);
    }

    private String buildFollowUpQuestion(Project project, String mode, String answer, AnswerEvaluation evaluation) {
        String lower = answer.toLowerCase(Locale.ROOT);

        if (containsAny(lower, List.of("jwt", "token", "登录"))) {
            return "你刚才提到了 JWT。请具体说一下：用户登录成功后 token 是怎么生成的？后端后续请求如何从 token 里拿到 userId？";
        }

        if (containsAny(lower, List.of("redis", "缓存"))) {
            return "你刚才提到了 Redis。请说明这个项目里 Redis 具体缓存了什么？key 怎么设计？过期时间怎么设置？";
        }

        if (containsAny(lower, List.of("规则扫描", "证据链", "readme"))) {
            return "你刚才提到了规则扫描或证据链。请解释一下：系统为什么不能只相信 README？它如何判断 README 中的技术表述是否有证据支撑？";
        }

        if (evaluation.score() < 65) {
            return "你的回答还比较泛。请你结合项目中的具体文件或类名，再说明一次你真正实现了哪些功能。";
        }

        return "如果面试官质疑这个项目是 AI 帮你生成的，你会如何证明自己理解了项目核心代码和设计思路？";
    }

    private int calculateTotalScore(List<InterviewMessage> userMessages) {
        if (userMessages == null || userMessages.isEmpty()) {
            return 0;
        }

        int sum = userMessages.stream()
                .map(InterviewMessage::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .sum();

        return Math.round((float) sum / userMessages.size());
    }

    private String buildSummary(int totalScore, int answerCount) {
        if (answerCount == 0) {
            return "本次面试没有有效回答，建议重新进行模拟面试。";
        }

        if (totalScore >= 80) {
            return "本次模拟面试整体表现较好，回答较具体，具备一定项目表达和抗追问能力。";
        }

        if (totalScore >= 60) {
            return "本次模拟面试表现中等，能够说明项目基本情况，但部分回答仍偏概括，需要加强具体实现细节。";
        }

        return "本次模拟面试风险较高，回答偏空泛，建议重点复习项目核心流程、关键代码和证据链设计。";
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

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private InterviewMessageVO toMessageVO(InterviewMessage message) {
        return InterviewMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .score(message.getScore())
                .feedback(message.getFeedback())
                .createTime(message.getCreateTime())
                .build();
    }

    private record AnswerEvaluation(int score, String feedback) {
    }
}