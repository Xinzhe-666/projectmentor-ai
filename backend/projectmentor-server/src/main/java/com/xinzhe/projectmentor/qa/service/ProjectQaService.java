package com.xinzhe.projectmentor.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiProperties;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.qa.entity.ProjectQaRecord;
import com.xinzhe.projectmentor.qa.mapper.ProjectQaRecordMapper;
import com.xinzhe.projectmentor.qa.vo.ProjectQaEvidenceVO;
import com.xinzhe.projectmentor.qa.vo.ProjectQaHistoryVO;
import com.xinzhe.projectmentor.qa.vo.ProjectQaResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectQaService {

    private static final int MAX_TOP_FILES = 8;

    private static final int MAX_EVIDENCE_COUNT = 8;

    private static final int MAX_SNIPPET_CHARS = 500;

    private static final int DEFAULT_MAX_PROMPT_CHARS = 12000;

    private static final int DEFAULT_HISTORY_LIMIT = 20;

    private static final TypeReference<List<ProjectQaEvidenceVO>> EVIDENCE_LIST_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<List<String>> FOLLOW_UP_LIST_TYPE = new TypeReference<>() {
    };

    private static final String NO_EVIDENCE_ANSWER = "当前项目文件中没有找到与该问题明显相关的证据。建议先上传 README 或项目 ZIP，或者换一个更具体的问题。";

    private static final String AI_UNAVAILABLE_ANSWER = "AI 当前不可用，下面是根据关键词检索到的相关项目证据。你可以根据这些文件继续追问或手动复盘。";

    private static final Pattern ENGLISH_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_+.#:-]{1,}");

    private static final Pattern CHINESE_TEXT_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");

    private static final Set<String> IMPLEMENTATION_PATH_KEYWORDS = Set.of(
            "controller", "service", "config", "util", "interceptor", "filter", "mapper", "entity"
    );

    private static final Set<String> CONFIG_PATH_KEYWORDS = Set.of(
            "application.yml", "application.yaml", "application.properties", "pom.xml",
            "package.json", "dockerfile", "docker-compose.yml", "docker-compose.yaml", ".sql"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "where", "what", "how", "why", "is", "are", "was", "were",
            "in", "on", "to", "for", "of", "with", "project", "code", "file", "files",
            "implement", "implemented", "implementation",
            "哪里", "在哪", "怎么", "如何", "什么", "这个", "项目", "实现", "一下",
            "是否", "有没有", "哪些", "当前", "相关", "代码", "文件", "说明", "讲讲"
    );

    private static final List<KeywordRule> KEYWORD_RULES = List.of(
            new KeywordRule(List.of("jwt"), 6, List.of("jwt", "token", "authorization", "bearer")),
            new KeywordRule(List.of("token", "令牌"), 5, List.of("token", "authorization", "bearer")),
            new KeywordRule(List.of("authorization", "鉴权", "认证", "权限"), 5, List.of("authorization", "auth", "permission", "token", "鉴权", "认证", "权限")),
            new KeywordRule(List.of("redis", "缓存"), 5, List.of("redis", "redistemplate", "cache", "缓存")),
            new KeywordRule(List.of("docker", "容器", "dockerfile", "docker-compose"), 5, List.of("docker", "dockerfile", "docker-compose.yml", "docker-compose.yaml")),
            new KeywordRule(List.of("mybatis", "mybatis-plus"), 5, List.of("mybatis", "mybatis-plus", "mapper", "basemapper")),
            new KeywordRule(List.of("分页"), 4, List.of("分页", "page", "pagination", "pagehelper", "ipage")),
            new KeywordRule(List.of("登录", "login"), 5, List.of("登录", "login", "auth", "token", "password", "jwt")),
            new KeywordRule(List.of("注册", "register"), 4, List.of("注册", "register", "username", "password")),
            new KeywordRule(List.of("拦截器", "interceptor"), 5, List.of("拦截器", "interceptor", "prehandle", "addinterceptors")),
            new KeywordRule(List.of("threadlocal"), 5, List.of("threadlocal", "usercontext")),
            new KeywordRule(List.of("ai", "大模型"), 4, List.of("ai", "llm", "openai", "chat", "大模型")),
            new KeywordRule(List.of("报告", "report"), 4, List.of("报告", "report", "analysis", "audit")),
            new KeywordRule(List.of("上传", "upload"), 4, List.of("上传", "upload", "multipartfile")),
            new KeywordRule(List.of("zip"), 5, List.of("zip", "zipinputstream", "zipentry")),
            new KeywordRule(List.of("额度", "credit"), 4, List.of("额度", "credit", "remainingcredits")),
            new KeywordRule(List.of("异步", "async"), 4, List.of("异步", "async", "threadpoolexecutor", "executor")),
            new KeywordRule(List.of("sse"), 4, List.of("sse", "eventsource", "text/event-stream")),
            new KeywordRule(List.of("nginx"), 4, List.of("nginx", "proxy_pass")),
            new KeywordRule(List.of("mysql"), 4, List.of("mysql", "datasource", "jdbc")),
            new KeywordRule(List.of("bcrypt"), 4, List.of("bcrypt", "passwordencoder")),
            new KeywordRule(List.of("简历", "resume"), 3, List.of("简历", "resume", "evidence", "证据")),
            new KeywordRule(List.of("面试", "interview", "追问"), 3, List.of("面试", "interview", "追问", "question")),
            new KeywordRule(List.of("配置", "config"), 3, List.of("配置", "config", "properties", "yml", "yaml")),
            new KeywordRule(List.of("接口", "api"), 3, List.of("接口", "api", "controller", "requestmapping"))
    );

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

    private final ProjectQaRecordMapper projectQaRecordMapper;

    private final LlmClient llmClient;

    private final AiProperties aiProperties;

    private final ObjectMapper objectMapper;

    public ProjectQaResponseVO ask(Long projectId, String question) {
        Long userId = getCurrentUserId();
        checkProjectOwner(projectId, userId);

        String normalizedQuestion = question == null ? "" : question.trim();
        List<ProjectFile> files = listProjectFiles(projectId);
        Map<String, Integer> keywords = extractKeywords(normalizedQuestion);
        List<ScoredFile> scoredFiles = scoreFiles(files, keywords);
        List<ProjectQaEvidenceVO> evidences = buildEvidences(scoredFiles, keywords);
        List<String> suggestedFollowUps = buildSuggestedFollowUps(keywords, evidences);
        EvidenceAssessment assessment = buildEvidenceAssessment(normalizedQuestion, evidences, scoredFiles);

        boolean aiUsed = false;
        String answer;

        if (evidences.isEmpty()) {
            answer = NO_EVIDENCE_ANSWER;
        } else {
            try {
                answer = askAi(normalizedQuestion, evidences);
                aiUsed = !answer.isBlank();
                if (!aiUsed) {
                    answer = AI_UNAVAILABLE_ANSWER;
                }
            } catch (Exception e) {
                log.info("Project QA AI unavailable, fallback to retrieval result: {}", e.getMessage());
                answer = AI_UNAVAILABLE_ANSWER;
            }
        }

        saveRecord(userId, projectId, normalizedQuestion, answer, aiUsed, evidences, suggestedFollowUps);

        return ProjectQaResponseVO.builder()
                .question(normalizedQuestion)
                .answer(answer)
                .aiUsed(aiUsed)
                .evidences(evidences)
                .suggestedFollowUps(suggestedFollowUps)
                .evidenceLevel(assessment.evidenceLevel())
                .evidenceLevelText(assessment.evidenceLevelText())
                .evidenceSummary(assessment.evidenceSummary())
                .interviewAnswer(assessment.interviewAnswer())
                .resumeRisk(assessment.resumeRisk())
                .confidenceScore(assessment.confidenceScore())
                .build();
    }

    public List<ProjectQaHistoryVO> listHistory(Long projectId) {
        Long userId = getCurrentUserId();
        checkProjectOwner(projectId, userId);

        List<ProjectQaRecord> records = projectQaRecordMapper.selectList(
                new LambdaQueryWrapper<ProjectQaRecord>()
                        .eq(ProjectQaRecord::getUserId, userId)
                        .eq(ProjectQaRecord::getProjectId, projectId)
                        .eq(ProjectQaRecord::getDeleted, 0)
                        .orderByDesc(ProjectQaRecord::getCreateTime)
                        .last("LIMIT " + DEFAULT_HISTORY_LIMIT)
        );

        return records.stream()
                .map(this::toHistoryVO)
                .toList();
    }

    public void deleteHistory(Long projectId, Long recordId) {
        Long userId = getCurrentUserId();
        checkProjectOwner(projectId, userId);

        int updated = projectQaRecordMapper.update(
                null,
                new LambdaUpdateWrapper<ProjectQaRecord>()
                        .eq(ProjectQaRecord::getId, recordId)
                        .eq(ProjectQaRecord::getUserId, userId)
                        .eq(ProjectQaRecord::getProjectId, projectId)
                        .eq(ProjectQaRecord::getDeleted, 0)
                        .set(ProjectQaRecord::getDeleted, 1)
        );

        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "问答记录不存在或无权限删除");
        }
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private void checkProjectOwner(Long projectId, Long userId) {
        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权访问");
        }
    }

    private List<ProjectFile> listProjectFiles(Long projectId) {
        return projectFileMapper.selectList(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
                        .orderByAsc(ProjectFile::getFilePath)
        );
    }

    private Map<String, Integer> extractKeywords(String question) {
        LinkedHashMap<String, Integer> keywords = new LinkedHashMap<>();
        String questionLower = question.toLowerCase(Locale.ROOT);

        Matcher englishMatcher = ENGLISH_TOKEN_PATTERN.matcher(question);
        while (englishMatcher.find()) {
            String token = englishMatcher.group().toLowerCase(Locale.ROOT);
            if (isUsefulToken(token)) {
                addKeyword(keywords, token, 2);
            }
        }

        Matcher chineseMatcher = CHINESE_TEXT_PATTERN.matcher(question);
        while (chineseMatcher.find()) {
            String text = chineseMatcher.group();
            for (KeywordRule rule : KEYWORD_RULES) {
                for (String trigger : rule.triggers()) {
                    if (containsIgnoreCase(text, trigger)) {
                        addKeyword(keywords, trigger, rule.weight());
                    }
                }
            }
        }

        for (KeywordRule rule : KEYWORD_RULES) {
            boolean matched = rule.triggers().stream()
                    .anyMatch(trigger -> containsIgnoreCase(questionLower, trigger));
            if (matched) {
                for (String alias : rule.aliases()) {
                    addKeyword(keywords, alias.toLowerCase(Locale.ROOT), rule.weight());
                }
            }
        }

        return keywords;
    }

    private boolean isUsefulToken(String token) {
        return token.length() >= 2 && !STOP_WORDS.contains(token);
    }

    private void addKeyword(Map<String, Integer> keywords, String keyword, int weight) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        if (STOP_WORDS.contains(normalized)) {
            return;
        }

        keywords.merge(normalized, weight, Math::max);
    }

    private List<ScoredFile> scoreFiles(List<ProjectFile> files, Map<String, Integer> keywords) {
        if (files == null || files.isEmpty() || keywords.isEmpty()) {
            return List.of();
        }

        List<ScoredFile> scoredFiles = new ArrayList<>();

        for (ProjectFile file : files) {
            String path = safe(file.getFilePath());
            String content = safe(file.getContent());
            String pathLower = path.toLowerCase(Locale.ROOT);
            String contentLower = content.toLowerCase(Locale.ROOT);
            List<String> pathHits = new ArrayList<>();
            List<String> contentHits = new ArrayList<>();
            int score = 0;

            for (Map.Entry<String, Integer> entry : keywords.entrySet()) {
                String keyword = entry.getKey();
                int weight = entry.getValue();

                if (pathContainsKeyword(pathLower, keyword)) {
                    score += 12 * weight;
                    pathHits.add(keyword);
                }

                int count = countKeywordMatches(contentLower, keyword);
                if (count > 0) {
                    int cappedCount = Math.min(count, 20);
                    score += cappedCount * 2 * weight;
                    contentHits.add(keyword + "(" + count + ")");
                }
            }

            int roleBoost = calculateRoleBoost(pathLower);
            if (score > 0) {
                score += roleBoost;
                scoredFiles.add(new ScoredFile(file, score, pathHits, contentHits, describeRole(pathLower)));
            }
        }

        if (scoredFiles.isEmpty() && isBroadReviewQuestion(keywords)) {
            return fallbackScoreFiles(files);
        }

        return scoredFiles.stream()
                .sorted(Comparator.comparingInt(ScoredFile::score).reversed()
                        .thenComparing(scoredFile -> safe(scoredFile.file().getFilePath())))
                .limit(MAX_TOP_FILES)
                .toList();
    }

    private boolean isBroadReviewQuestion(Map<String, Integer> keywords) {
        return containsAnyKeyword(keywords, List.of("简历", "resume", "面试", "interview", "追问", "证据"));
    }

    private List<ScoredFile> fallbackScoreFiles(List<ProjectFile> files) {
        return files.stream()
                .map(file -> {
                    String pathLower = safe(file.getFilePath()).toLowerCase(Locale.ROOT);
                    int roleBoost = calculateRoleBoost(pathLower);
                    if (pathLower.contains("readme")) {
                        roleBoost += 8;
                    }
                    return new ScoredFile(
                            file,
                            roleBoost,
                            List.of(),
                            List.of(),
                            "综合复盘问题，优先选取 README 或核心代码文件作为证据"
                    );
                })
                .filter(scoredFile -> scoredFile.score() > 0)
                .sorted(Comparator.comparingInt(ScoredFile::score).reversed()
                        .thenComparing(scoredFile -> safe(scoredFile.file().getFilePath())))
                .limit(MAX_TOP_FILES)
                .toList();
    }

    private int calculateRoleBoost(String pathLower) {
        if (pathLower.contains("controller")) {
            return 10;
        }
        if (pathLower.contains("service")) {
            return 9;
        }
        if (pathLower.contains("config") || pathLower.contains("interceptor")
                || pathLower.contains("filter") || pathLower.contains("util")) {
            return 8;
        }
        if (pathLower.contains("mapper") || pathLower.contains("entity")) {
            return 6;
        }
        if (pathLower.endsWith("pom.xml") || pathLower.endsWith("application.yml")
                || pathLower.endsWith("application.yaml") || pathLower.endsWith("application.properties")) {
            return 7;
        }
        if (pathLower.endsWith("dockerfile") || pathLower.endsWith("docker-compose.yml")
                || pathLower.endsWith("docker-compose.yaml")) {
            return 7;
        }
        if (pathLower.endsWith("package.json") || pathLower.endsWith(".sql")) {
            return 5;
        }
        if (pathLower.contains("readme")) {
            return 3;
        }
        return 0;
    }

    private String describeRole(String pathLower) {
        if (pathLower.contains("controller")) {
            return "Controller 文件通常对应接口入口";
        }
        if (pathLower.contains("service")) {
            return "Service 文件通常对应核心业务逻辑";
        }
        if (pathLower.contains("config")) {
            return "Config 文件通常对应项目配置";
        }
        if (pathLower.contains("interceptor")) {
            return "Interceptor 文件通常对应请求拦截逻辑";
        }
        if (pathLower.contains("filter")) {
            return "Filter 文件通常对应过滤逻辑";
        }
        if (pathLower.contains("util")) {
            return "Util 文件通常对应工具类实现";
        }
        if (pathLower.contains("mapper")) {
            return "Mapper 文件通常对应数据访问逻辑";
        }
        if (pathLower.contains("entity")) {
            return "Entity 文件通常对应数据模型";
        }
        if (pathLower.contains("readme")) {
            return "README 可作为项目说明参考";
        }
        if (pathLower.endsWith("pom.xml") || pathLower.endsWith("package.json")
                || pathLower.endsWith("application.yml") || pathLower.endsWith("application.yaml")
                || pathLower.endsWith("application.properties") || pathLower.endsWith(".sql")
                || pathLower.endsWith("dockerfile") || pathLower.endsWith("docker-compose.yml")
                || pathLower.endsWith("docker-compose.yaml")) {
            return "配置文件可作为依赖、部署或运行证据";
        }
        return "";
    }

    private boolean pathContainsKeyword(String pathLower, String keyword) {
        if ("ai".equals(keyword)) {
            return countAsciiWordMatches(pathLower, keyword) > 0;
        }
        return pathLower.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private int countKeywordMatches(String textLower, String keyword) {
        if (textLower.isBlank() || keyword.isBlank()) {
            return 0;
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        if (isShortAsciiKeyword(normalizedKeyword)) {
            return countAsciiWordMatches(textLower, normalizedKeyword);
        }

        int count = 0;
        int index = 0;
        while (index < textLower.length()) {
            int found = textLower.indexOf(normalizedKeyword, index);
            if (found < 0) {
                break;
            }
            count++;
            index = found + normalizedKeyword.length();
        }
        return count;
    }

    private boolean isShortAsciiKeyword(String keyword) {
        return keyword.length() <= 2 && keyword.chars().allMatch(ch -> ch < 128 && Character.isLetterOrDigit(ch));
    }

    private int countAsciiWordMatches(String textLower, String keyword) {
        Pattern pattern = Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(keyword) + "(?![a-z0-9_])");
        Matcher matcher = pattern.matcher(textLower);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private List<ProjectQaEvidenceVO> buildEvidences(List<ScoredFile> scoredFiles, Map<String, Integer> keywords) {
        if (scoredFiles.isEmpty()) {
            return List.of();
        }

        List<ProjectQaEvidenceVO> evidences = new ArrayList<>();
        for (ScoredFile scoredFile : scoredFiles) {
            if (evidences.size() >= MAX_EVIDENCE_COUNT) {
                break;
            }

            String snippet = extractSnippet(safe(scoredFile.file().getContent()), keywords.keySet());
            if (snippet.isBlank()) {
                continue;
            }

            evidences.add(ProjectQaEvidenceVO.builder()
                    .filePath(scoredFile.file().getFilePath())
                    .reason(buildReason(scoredFile))
                    .snippet(snippet)
                    .build());
        }

        return evidences;
    }

    private String buildReason(ScoredFile scoredFile) {
        List<String> reasons = new ArrayList<>();

        if (!scoredFile.pathHits().isEmpty()) {
            reasons.add("文件路径命中 " + joinDistinct(scoredFile.pathHits()));
        }
        if (!scoredFile.contentHits().isEmpty()) {
            reasons.add("文件内容命中 " + joinDistinct(scoredFile.contentHits()));
        }
        if (!scoredFile.roleReason().isBlank()) {
            reasons.add(scoredFile.roleReason());
        }

        return String.join("；", reasons);
    }

    private String extractSnippet(String content, Set<String> keywords) {
        if (content.isBlank()) {
            return "";
        }

        String contentLower = content.toLowerCase(Locale.ROOT);
        int bestIndex = -1;

        for (String keyword : keywords) {
            int index = contentLower.indexOf(keyword.toLowerCase(Locale.ROOT));
            if (index >= 0 && (bestIndex < 0 || index < bestIndex)) {
                bestIndex = index;
            }
        }

        if (bestIndex < 0) {
            bestIndex = 0;
        }

        int start = Math.max(0, bestIndex - MAX_SNIPPET_CHARS / 3);
        int end = Math.min(content.length(), start + MAX_SNIPPET_CHARS);
        String snippet = content.substring(start, end).trim();

        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }

        return truncate(snippet, MAX_SNIPPET_CHARS);
    }

    private String askAi(String question, List<ProjectQaEvidenceVO> evidences) {
        String systemPrompt = """
                你是 ProjectMentor AI 的项目问答助手，面向计算机学生和后端实习候选人。
                你只能根据用户提供的证据片段回答，不允许编造不存在的功能。
                如果证据不足，必须明确说：“当前上传文件中没有找到足够证据。”
                如果只有 README 描述、但缺少代码或配置证据，必须明确说：“README 中有描述，但当前上传代码证据不足。”
                回答必须包含：直接回答、证据依据、面试讲法、简历风险提示、可能被追问的问题。
                不允许夸大为高并发、分布式、企业级生产系统，除非证据明确支持。
                不允许把没有证据的猜测说成事实。
                不允许把证据不足的问题包装成已经完整实现。
                当前能力只是基于已上传文件的轻量检索增强问答，不要描述成成熟向量检索或成熟 RAG 系统。
                """;

        String userPrompt = buildUserPrompt(question, evidences, resolveMaxPromptChars() - systemPrompt.length());
        return llmClient.chat("PROJECT_QA", systemPrompt, userPrompt);
    }

    private String buildUserPrompt(String question, List<ProjectQaEvidenceVO> evidences, int maxChars) {
        String header = """
                用户问题：
                %s

                请基于下面证据回答。证据以文件路径、命中原因、片段组成：
                """.formatted(question);

        StringBuilder builder = new StringBuilder(header);
        int limit = Math.max(3000, maxChars - 500);

        for (ProjectQaEvidenceVO evidence : evidences) {
            String block = """

                    [文件路径]
                    %s
                    [命中原因]
                    %s
                    [证据片段]
                    %s
                    """.formatted(
                    safe(evidence.getFilePath()),
                    safe(evidence.getReason()),
                    safe(evidence.getSnippet())
            );

            if (builder.length() + block.length() > limit) {
                break;
            }
            builder.append(block);
        }

        return builder.toString();
    }

    private int resolveMaxPromptChars() {
        Integer maxPromptChars = aiProperties.getMaxPromptChars();
        return maxPromptChars == null || maxPromptChars <= 0 ? DEFAULT_MAX_PROMPT_CHARS : maxPromptChars;
    }

    private List<String> buildSuggestedFollowUps(Map<String, Integer> keywords, List<ProjectQaEvidenceVO> evidences) {
        LinkedHashSet<String> followUps = new LinkedHashSet<>();

        if (containsAnyKeyword(keywords, List.of("jwt", "token", "authorization", "登录", "login"))) {
            followUps.add("JWT 过期时间在哪里配置？");
            followUps.add("登录后 token 是如何返回给前端的？");
        }
        if (containsAnyKeyword(keywords, List.of("redis", "缓存"))) {
            followUps.add("Redis 在这个项目中承担什么作用？");
        }
        if (containsAnyKeyword(keywords, List.of("upload", "上传", "zip"))) {
            followUps.add("ZIP 上传的安全限制在哪里实现？");
        }
        if (containsAnyKeyword(keywords, List.of("docker", "nginx"))) {
            followUps.add("部署相关文件能证明哪些能力？");
        }
        if (containsAnyKeyword(keywords, List.of("简历", "resume", "面试", "interview"))) {
            followUps.add("哪些文件最适合作为面试证据？");
            followUps.add("面试官可能继续追问哪些实现细节？");
        }

        followUps.add("这个功能的接口入口在哪里？");
        followUps.add("当前证据还缺少哪些文件？");

        int maxCount = evidences.isEmpty() ? 2 : 4;
        return followUps.stream().limit(maxCount).toList();
    }

    private boolean containsAnyKeyword(Map<String, Integer> keywords, List<String> candidates) {
        for (String candidate : candidates) {
            if (keywords.containsKey(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private EvidenceAssessment buildEvidenceAssessment(String question,
                                                       List<ProjectQaEvidenceVO> evidences,
                                                       List<ScoredFile> scoredFiles) {
        EvidenceStats stats = buildEvidenceStats(evidences, scoredFiles);
        String keyFiles = summarizeEvidencePaths(evidences);

        String evidenceLevel;
        int confidenceScore;

        if (stats.evidenceCount() == 0 || stats.contentHitCount() == 0) {
            evidenceLevel = "NONE";
            confidenceScore = clamp(18 + stats.evidenceCount() * 3, 0, 30);
        } else if (stats.readmeOnly() || stats.implementationEvidenceCount() == 0 && stats.configEvidenceCount() == 0) {
            evidenceLevel = "WEAK";
            confidenceScore = clamp(36 + stats.evidenceCount() * 4 + stats.contentHitCount() * 3, 31, 55);
        } else if (stats.implementationEvidenceCount() >= 3 && stats.pathAndContentHitCount() >= 2) {
            evidenceLevel = "STRONG";
            confidenceScore = clamp(78 + stats.implementationEvidenceCount() * 4 + stats.pathAndContentHitCount() * 3, 76, 95);
        } else if (stats.implementationEvidenceCount() >= 1
                || stats.hasReadmeEvidence() && stats.configEvidenceCount() > 0 && stats.contentHitCount() >= 2) {
            evidenceLevel = "MEDIUM";
            confidenceScore = clamp(58 + stats.implementationEvidenceCount() * 5
                    + stats.configEvidenceCount() * 3 + stats.contentHitCount() * 2, 56, 75);
        } else {
            evidenceLevel = "WEAK";
            confidenceScore = clamp(35 + stats.evidenceCount() * 4, 31, 55);
        }

        String evidenceLevelText = evidenceLevelText(evidenceLevel);
        String evidenceSummary = buildEvidenceSummary(evidenceLevel, stats);
        String interviewAnswer = buildInterviewAnswer(question, evidenceLevel, keyFiles);
        String resumeRisk = buildResumeRisk(evidenceLevel, stats);

        return new EvidenceAssessment(
                evidenceLevel,
                evidenceLevelText,
                evidenceSummary,
                interviewAnswer,
                resumeRisk,
                confidenceScore
        );
    }

    private EvidenceStats buildEvidenceStats(List<ProjectQaEvidenceVO> evidences, List<ScoredFile> scoredFiles) {
        int evidenceCount = evidences == null ? 0 : evidences.size();
        if (evidenceCount == 0) {
            return new EvidenceStats(0, 0, 0, 0, false, false, 0);
        }

        int reasonContentHitCount = 0;
        int reasonPathAndContentHitCount = 0;
        int implementationEvidenceCount = 0;
        int configEvidenceCount = 0;
        boolean hasReadmeEvidence = false;
        Set<String> evidencePaths = new LinkedHashSet<>();

        for (ProjectQaEvidenceVO evidence : evidences) {
            String path = safe(evidence.getFilePath()).toLowerCase(Locale.ROOT);
            String reason = safe(evidence.getReason());
            evidencePaths.add(path);

            if (isReadmePath(path)) {
                hasReadmeEvidence = true;
            }
            if (isImplementationPath(path)) {
                implementationEvidenceCount++;
            }
            if (isConfigPath(path)) {
                configEvidenceCount++;
            }
            if (reason.contains("文件内容命中")) {
                reasonContentHitCount++;
            }
            if (reason.contains("文件路径命中") && reason.contains("文件内容命中")) {
                reasonPathAndContentHitCount++;
            }
        }

        int scoredContentHitCount = 0;
        int scoredPathAndContentHitCount = 0;
        if (scoredFiles != null && !scoredFiles.isEmpty()) {
            for (ScoredFile scoredFile : scoredFiles) {
                String path = safe(scoredFile.file().getFilePath()).toLowerCase(Locale.ROOT);
                if (!evidencePaths.contains(path)) {
                    continue;
                }
                if (!scoredFile.contentHits().isEmpty()) {
                    scoredContentHitCount++;
                }
                if (!scoredFile.pathHits().isEmpty() && !scoredFile.contentHits().isEmpty()) {
                    scoredPathAndContentHitCount++;
                }
            }
        }

        int contentHitCount = Math.max(reasonContentHitCount, scoredContentHitCount);
        int pathAndContentHitCount = Math.max(reasonPathAndContentHitCount, scoredPathAndContentHitCount);
        boolean readmeOnly = hasReadmeEvidence && implementationEvidenceCount == 0 && configEvidenceCount == 0;

        return new EvidenceStats(
                evidenceCount,
                contentHitCount,
                pathAndContentHitCount,
                implementationEvidenceCount,
                hasReadmeEvidence,
                readmeOnly,
                configEvidenceCount
        );
    }

    private String evidenceLevelText(String evidenceLevel) {
        return switch (evidenceLevel) {
            case "STRONG" -> "强证据";
            case "MEDIUM" -> "中等证据";
            case "WEAK" -> "弱证据";
            default -> "证据不足";
        };
    }

    private String buildEvidenceSummary(String evidenceLevel, EvidenceStats stats) {
        if ("NONE".equals(evidenceLevel)) {
            if (stats.evidenceCount() == 0) {
                return "当前上传文件中没有找到明显相关证据，无法支撑完整回答。";
            }
            return "找到了少量文件线索，但当前证据片段没有命中核心问题关键词，证据仍不足。";
        }

        if ("WEAK".equals(evidenceLevel)) {
            if (stats.readmeOnly()) {
                return "当前证据主要来自 README 描述，缺少代码、配置或实现链路支撑。";
            }
            return "当前证据较少或较泛，尚不足以证明完整实现链路。";
        }

        if ("MEDIUM".equals(evidenceLevel)) {
            return "当前找到 " + stats.evidenceCount() + " 条证据，包含 "
                    + stats.implementationEvidenceCount() + " 个代码实现文件线索，可以支撑部分回答，但链路仍需结合代码继续复盘。";
        }

        return "当前找到 " + stats.evidenceCount() + " 条证据，包含多个关键代码文件，且路径和内容均有命中，能较好支撑本次回答。";
    }

    private String buildInterviewAnswer(String question, String evidenceLevel, String keyFiles) {
        String filesText = keyFiles.isBlank() ? "当前上传材料" : keyFiles;

        return switch (evidenceLevel) {
            case "STRONG" -> "面试中可以先直接回答问题，再结合 " + filesText
                    + " 说明入口、核心逻辑和边界。注意只讲当前代码能证明的部分，不扩展成没有证据的生产级能力。";
            case "MEDIUM" -> "面试中可以说这个点在 " + filesText
                    + " 中有实现或配置线索，然后说明自己理解的流程。遇到细节追问时，要回到文件路径和片段讲，不要说成完整闭环已经充分证明。";
            case "WEAK" -> "面试中建议保守表达：当前材料只找到 " + filesText
                    + " 这类线索，可以作为说明或初步实现参考，但不要把它包装成独立设计或完整能力。";
            default -> "面试中建议先说明当前上传文件中没有找到足够证据，不能确认该点已经完整实现。可以补充 README、核心代码或演示截图后再展开说明。";
        };
    }

    private String buildResumeRisk(String evidenceLevel, EvidenceStats stats) {
        if ("NONE".equals(evidenceLevel)) {
            return "当前证据不足，不建议写进简历，也不建议在面试中主动夸大这个点。";
        }
        if ("WEAK".equals(evidenceLevel)) {
            if (stats.readmeOnly()) {
                return "README 有描述，但当前上传代码证据不足，需要补充实现或演示截图后再考虑写进简历。";
            }
            return "不建议夸大为独立设计；如果写进简历，需要用更保守的表述，并准备补充代码证据。";
        }
        if ("MEDIUM".equals(evidenceLevel)) {
            return "可以谨慎写，但需要能讲清楚实现细节、文件位置和当前证据边界。";
        }
        return "可以写，但仍要按当前证据范围描述，并准备被追问接口入口、核心逻辑和异常边界。";
    }

    private String summarizeEvidencePaths(List<ProjectQaEvidenceVO> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "";
        }

        return evidences.stream()
                .map(evidence -> safe(evidence.getFilePath()))
                .filter(path -> !path.isBlank())
                .distinct()
                .limit(3)
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
    }

    private boolean isReadmePath(String pathLower) {
        return pathLower.contains("readme");
    }

    private boolean isImplementationPath(String pathLower) {
        return IMPLEMENTATION_PATH_KEYWORDS.stream().anyMatch(pathLower::contains);
    }

    private boolean isConfigPath(String pathLower) {
        return CONFIG_PATH_KEYWORDS.stream().anyMatch(pathLower::contains);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void saveRecord(Long userId,
                            Long projectId,
                            String question,
                            String answer,
                            boolean aiUsed,
                            List<ProjectQaEvidenceVO> evidences,
                            List<String> suggestedFollowUps) {
        try {
            ProjectQaRecord record = new ProjectQaRecord();
            record.setUserId(userId);
            record.setProjectId(projectId);
            record.setQuestion(question);
            record.setAnswer(answer);
            record.setAiUsed(aiUsed ? 1 : 0);
            record.setEvidenceJson(toJson(evidences));
            record.setSuggestedFollowUpsJson(toJson(suggestedFollowUps));
            record.setDeleted(0);

            projectQaRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("Failed to save project QA record: projectId={}, message={}", projectId, e.getMessage());
        }
    }

    private ProjectQaHistoryVO toHistoryVO(ProjectQaRecord record) {
        List<ProjectQaEvidenceVO> evidences = parseJsonList(record.getEvidenceJson(), EVIDENCE_LIST_TYPE);
        List<String> suggestedFollowUps = parseJsonList(record.getSuggestedFollowUpsJson(), FOLLOW_UP_LIST_TYPE);
        EvidenceAssessment assessment = buildEvidenceAssessment(record.getQuestion(), evidences, List.of());

        return ProjectQaHistoryVO.builder()
                .id(record.getId())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .aiUsed(record.getAiUsed() != null && record.getAiUsed() == 1)
                .evidences(evidences)
                .suggestedFollowUps(suggestedFollowUps)
                .createTime(record.getCreateTime())
                .evidenceLevel(assessment.evidenceLevel())
                .evidenceLevelText(assessment.evidenceLevelText())
                .evidenceSummary(assessment.evidenceSummary())
                .interviewAnswer(assessment.interviewAnswer())
                .resumeRisk(assessment.resumeRisk())
                .confidenceScore(assessment.confidenceScore())
                .build();
    }

    private <T> List<T> parseJsonList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<T> values = objectMapper.readValue(json, typeReference);
            return values == null ? List.of() : values;
        } catch (Exception e) {
            log.warn("Failed to parse project QA history json: {}", e.getMessage());
            return List.of();
        }
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String joinDistinct(List<String> values) {
        return String.join("、", new LinkedHashSet<>(values));
    }

    private String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private record KeywordRule(List<String> triggers, int weight, List<String> aliases) {
    }

    private record ScoredFile(ProjectFile file,
                              int score,
                              List<String> pathHits,
                              List<String> contentHits,
                              String roleReason) {
    }

    private record EvidenceAssessment(String evidenceLevel,
                                      String evidenceLevelText,
                                      String evidenceSummary,
                                      String interviewAnswer,
                                      String resumeRisk,
                                      Integer confidenceScore) {
    }

    private record EvidenceStats(int evidenceCount,
                                 int contentHitCount,
                                 int pathAndContentHitCount,
                                 int implementationEvidenceCount,
                                 boolean hasReadmeEvidence,
                                 boolean readmeOnly,
                                 int configEvidenceCount) {
    }
}
