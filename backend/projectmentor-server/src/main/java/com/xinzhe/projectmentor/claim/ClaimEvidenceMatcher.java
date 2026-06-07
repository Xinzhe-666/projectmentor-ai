package com.xinzhe.projectmentor.claim;

import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceFileVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ClaimEvidenceMatcher {

    private static final int MAX_EVIDENCE_FILES = 5;
    private static final int MAX_INDEXED_CONTENT_CHARS = 60000;
    private static final int MAX_SNIPPET_CHARS = 300;

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9_.-]{2,}|[\\p{IsHan}]{2,8}"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "项目", "系统", "功能", "支持", "实现", "完成", "使用", "基于", "进行", "相关",
            "当前", "可以", "以及", "通过", "包含", "技术栈", "project", "system", "feature",
            "support", "using", "based", "with", "from", "this", "that"
    );

    private static final List<String> RISK_KEYWORDS = List.of(
            "高并发", "企业级", "生产级", "分布式", "微服务", "高可用", "百万用户",
            "qps", "商业化", "完整支付", "真实支付", "海量数据", "大规模"
    );

    private static final Map<ClaimCategory, List<String>> CATEGORY_KEYWORDS = buildCategoryKeywords();

    private final ClaimTextSanitizer sanitizer;

    public List<ClaimEvidenceItemVO> matchAll(List<ClaimEvidenceItemVO> claims, List<ProjectFile> files) {
        List<IndexedFile> indexedFiles = files.stream()
                .map(this::indexFile)
                .toList();

        return claims.stream()
                .map(claim -> matchIndexed(claim, indexedFiles))
                .toList();
    }

    public ClaimEvidenceItemVO match(ClaimEvidenceItemVO claim, List<ProjectFile> files) {
        return matchIndexed(claim, files.stream().map(this::indexFile).toList());
    }

    private ClaimEvidenceItemVO matchIndexed(ClaimEvidenceItemVO claim, List<IndexedFile> files) {
        List<String> keywords = buildKeywords(claim);
        List<EvidenceMatch> matches = new ArrayList<>();

        for (IndexedFile file : files) {
            EvidenceMatch match = matchFile(file, keywords);
            if (match != null) {
                matches.add(match);
            }
        }

        matches.sort(Comparator
                .comparing(EvidenceMatch::strong).reversed()
                .thenComparing(EvidenceMatch::score, Comparator.reverseOrder())
                .thenComparing(match -> safe(match.file().getFilePath())));

        List<EvidenceMatch> selected = matches.stream()
                .limit(MAX_EVIDENCE_FILES)
                .toList();
        long strongCount = matches.stream().filter(EvidenceMatch::strong).count();
        long weakCount = matches.size() - strongCount;
        int topScore = matches.isEmpty() ? 0 : matches.get(0).score();
        boolean riskyWording = containsAny(claim.getClaimText(), RISK_KEYWORDS);
        boolean robustRiskEvidence = hasRobustRiskEvidence(claim, matches);

        ClaimEvidenceStatus status = decideStatus(
                riskyWording,
                robustRiskEvidence,
                strongCount,
                weakCount,
                topScore
        );
        int confidenceScore = calculateConfidence(status, strongCount, weakCount, topScore);

        return ClaimEvidenceItemVO.builder()
                .claimText(claim.getClaimText())
                .sourceType(claim.getSourceType())
                .sourceSnippet(claim.getSourceSnippet())
                .category(claim.getCategory())
                .status(status)
                .confidenceScore(confidenceScore)
                .reason(buildReason(status, strongCount, weakCount))
                .evidenceFiles(selected.stream().map(this::toVO).toList())
                .resumeAdvice(buildResumeAdvice(status))
                .interviewQuestion(buildInterviewQuestion(claim, status))
                .build();
    }

    private IndexedFile indexFile(ProjectFile file) {
        String content = safe(file.getContent());
        String indexedContent = content.length() <= MAX_INDEXED_CONTENT_CHARS
                ? content
                : content.substring(0, MAX_INDEXED_CONTENT_CHARS);

        return new IndexedFile(
                file,
                safe(file.getFilePath()).toLowerCase(Locale.ROOT),
                indexedContent,
                indexedContent.toLowerCase(Locale.ROOT),
                isStrongEvidence(file)
        );
    }

    private EvidenceMatch matchFile(IndexedFile indexedFile, List<String> keywords) {
        int score = 0;
        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            boolean pathMatched = indexedFile.pathLower().contains(lowerKeyword);
            boolean contentMatched = indexedFile.contentLower().contains(lowerKeyword);

            if (!pathMatched && !contentMatched) {
                continue;
            }

            matchedKeywords.add(keyword);
            if (pathMatched) {
                score += indexedFile.strong() ? 3 : 2;
            }
            if (contentMatched) {
                score += indexedFile.strong() ? 2 : 1;
            }
        }

        if (score < (indexedFile.strong() ? 2 : 1)) {
            return null;
        }

        return new EvidenceMatch(
                indexedFile.file(),
                indexedFile.strong(),
                score,
                matchedKeywords.stream().distinct().limit(6).toList(),
                buildSnippet(indexedFile.content(), matchedKeywords)
        );
    }

    private List<String> buildKeywords(ClaimEvidenceItemVO claim) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>(
                CATEGORY_KEYWORDS.getOrDefault(claim.getCategory(), List.of())
        );

        Matcher matcher = TOKEN_PATTERN.matcher(claim.getClaimText().toLowerCase(Locale.ROOT));
        while (matcher.find() && keywords.size() < 28) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }

        return keywords.stream()
                .filter(keyword -> keyword.length() >= 2)
                .toList();
    }

    private ClaimEvidenceStatus decideStatus(boolean riskyWording,
                                             boolean robustRiskEvidence,
                                             long strongCount,
                                             long weakCount,
                                             int topScore) {
        if (riskyWording && !robustRiskEvidence) {
            return ClaimEvidenceStatus.RISKY;
        }
        if (strongCount >= 2 || (strongCount >= 1 && topScore >= 8)) {
            return ClaimEvidenceStatus.SUPPORTED;
        }
        if (strongCount >= 1) {
            return ClaimEvidenceStatus.PARTIAL;
        }
        if (weakCount >= 1) {
            return ClaimEvidenceStatus.DOC_ONLY;
        }
        return ClaimEvidenceStatus.NO_EVIDENCE;
    }

    private boolean hasRobustRiskEvidence(ClaimEvidenceItemVO claim, List<EvidenceMatch> matches) {
        List<EvidenceMatch> strongMatches = matches.stream()
                .filter(EvidenceMatch::strong)
                .toList();
        if (strongMatches.size() < 2) {
            return false;
        }

        String combined = strongMatches.stream()
                .limit(10)
                .map(match -> safe(match.file().getFilePath()) + " " + safe(match.file().getContent()))
                .map(text -> text.length() <= MAX_INDEXED_CONTENT_CHARS
                        ? text
                        : text.substring(0, MAX_INDEXED_CONTENT_CHARS))
                .collect(java.util.stream.Collectors.joining("\n"))
                .toLowerCase(Locale.ROOT);

        if (claim.getCategory() == ClaimCategory.PERFORMANCE) {
            return containsAny(combined, List.of(
                    "jmeter", "wrk", "benchmark", "压测", "qps", "ratelimiter", "sentinel"
            ));
        }

        if (claim.getClaimText().contains("支付")) {
            return containsAny(combined, List.of(
                    "payment", "支付回调", "webhook", "订单", "alipay", "wechatpay", "stripe"
            ));
        }

        return true;
    }

    private int calculateConfidence(ClaimEvidenceStatus status,
                                    long strongCount,
                                    long weakCount,
                                    int topScore) {
        int score = switch (status) {
            case SUPPORTED -> 72;
            case PARTIAL -> 48;
            case DOC_ONLY -> 30;
            case NO_EVIDENCE -> 10;
            case RISKY -> 18;
        };

        score += (int) Math.min(18, strongCount * 7);
        score += (int) Math.min(6, weakCount * 2);
        score += Math.min(8, topScore / 2);
        return Math.max(0, Math.min(98, score));
    }

    private String buildReason(ClaimEvidenceStatus status, long strongCount, long weakCount) {
        return switch (status) {
            case SUPPORTED -> "找到 " + strongCount + " 个代码、配置或部署类强证据，当前主张具备较完整支撑。";
            case PARTIAL -> "找到部分实现证据，但证据维度或实现链路仍不完整，建议结合实际贡献补充说明。";
            case DOC_ONLY -> "当前只找到 " + weakCount + " 个 README 或文档类弱证据，未发现对应代码、配置或部署实现。";
            case NO_EVIDENCE -> "当前已上传项目文件中未找到与该主张直接相关的有效证据。";
            case RISKY -> "该主张包含高风险或强结论表述，现有实现、测试或运行数据不足以支撑其强度。";
        };
    }

    private String buildResumeAdvice(ClaimEvidenceStatus status) {
        return switch (status) {
            case SUPPORTED -> "可以在简历中使用，但应保持与证据文件和个人真实贡献一致。";
            case PARTIAL -> "建议降低确定性，改为“参与实现”“完成基础能力”或明确当前实现边界。";
            case DOC_ONLY -> "不建议直接写成已实现能力；先补充代码、配置或部署证据。";
            case NO_EVIDENCE -> "暂不建议写入简历，除非能补充可验证的实现材料。";
            case RISKY -> "建议删除夸大词和规模结论，改写为可验证的具体功能、测试结果或工程措施。";
        };
    }

    private String buildInterviewQuestion(ClaimEvidenceItemVO claim, ClaimEvidenceStatus status) {
        String base = switch (claim.getCategory()) {
            case AUTH -> "请结合登录接口、Token 生成和拦截器说明完整鉴权链路。";
            case DATABASE -> "请指出相关表、实体和 Mapper，并说明数据读写或事务边界。";
            case CACHE -> "Redis 或缓存具体保存什么数据，失效策略和降级方式是什么？";
            case AI -> "AI 调用入口、失败降级和结果约束分别在哪里实现？";
            case RAG_OR_QA -> "检索候选、排序和证据引用如何实现，与向量 RAG 的边界是什么？";
            case FILE_UPLOAD -> "上传大小、文件过滤、解析边界和异常处理如何保证？";
            case REPORT -> "报告由哪些规则和证据生成，结论如何追溯到文件？";
            case INTERVIEW -> "面试问题如何生成、推进、跳过并形成复盘结果？";
            case ADMIN -> "管理员身份如何校验，普通用户为什么无法访问后台接口？";
            case CREDIT -> "额度扣减、失败返还和流水记录如何保证一致性？";
            case DEPLOYMENT -> "请结合 Docker、Nginx 或脚本说明完整部署与恢复流程。";
            case FRONTEND -> "请指出对应页面、路由和 API 调用，并说明状态如何展示。";
            case SECURITY -> "安全措施具体拦截什么风险，后端鉴权和配置脱敏如何配合？";
            case PERFORMANCE -> "该性能结论来自什么压测环境、指标、样本和瓶颈分析？";
            case BUSINESS_OR_PRODUCT -> "该产品成果有哪些真实用户、运行数据或业务记录可以证明？";
            case GENERAL -> "请指出最直接的实现文件，并说明该主张的真实边界。";
        };

        if (status == ClaimEvidenceStatus.SUPPORTED) {
            return base;
        }

        return base + " 当前证据不足时，请主动说明这是部分实现、文档目标还是后续计划。";
    }

    private ClaimEvidenceFileVO toVO(EvidenceMatch match) {
        ProjectFile file = match.file();
        return ClaimEvidenceFileVO.builder()
                .fileId(file.getId())
                .filePath(file.getFilePath())
                .fileType(file.getFileType())
                .evidenceLevel(match.strong() ? "STRONG" : "WEAK")
                .matchedKeywords(match.matchedKeywords())
                .snippet(match.snippet())
                .reason(match.strong()
                        ? "文件路径或内容命中主张关键词，且属于代码、配置、SQL、前端或部署实现文件。"
                        : "文件路径或内容命中主张关键词，但属于 README 或说明性文档。")
                .build();
    }

    private String buildSnippet(String content, List<String> matchedKeywords) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String lower = content.toLowerCase(Locale.ROOT);
        int matchIndex = -1;
        for (String keyword : matchedKeywords) {
            matchIndex = lower.indexOf(keyword.toLowerCase(Locale.ROOT));
            if (matchIndex >= 0) {
                break;
            }
        }

        if (matchIndex < 0) {
            return "";
        }

        int start = Math.max(0, matchIndex - 110);
        int end = Math.min(content.length(), start + MAX_SNIPPET_CHARS);
        if (end - start < MAX_SNIPPET_CHARS && start > 0) {
            start = Math.max(0, end - MAX_SNIPPET_CHARS);
        }

        return sanitizer.sanitizeAndLimit(content.substring(start, end), MAX_SNIPPET_CHARS);
    }

    private boolean isStrongEvidence(ProjectFile file) {
        String path = safe(file.getFilePath()).toLowerCase(Locale.ROOT).replace('\\', '/');
        String type = safe(file.getFileType()).toLowerCase(Locale.ROOT);

        if (path.equals("readme.md")
                || path.endsWith("/readme.md")
                || path.startsWith("docs/")
                || path.contains("/docs/")
                || path.endsWith(".md")
                || "readme".equals(type)
                || "doc".equals(type)) {
            return false;
        }

        return path.contains("/src/")
                || path.startsWith("src/")
                || path.startsWith("scripts/")
                || path.contains("/scripts/")
                || path.endsWith(".java")
                || path.endsWith(".kt")
                || path.endsWith(".ts")
                || path.endsWith(".tsx")
                || path.endsWith(".js")
                || path.endsWith(".vue")
                || path.endsWith(".sql")
                || path.endsWith(".yml")
                || path.endsWith(".yaml")
                || path.endsWith(".properties")
                || path.endsWith(".xml")
                || path.endsWith(".sh")
                || path.endsWith(".ps1")
                || path.endsWith(".json")
                || path.endsWith("dockerfile")
                || path.endsWith("nginx.conf")
                || path.endsWith("pom.xml");
    }

    private static Map<ClaimCategory, List<String>> buildCategoryKeywords() {
        Map<ClaimCategory, List<String>> keywords = new EnumMap<>(ClaimCategory.class);
        keywords.put(ClaimCategory.AUTH, List.of(
                "jwt", "jwtutil", "auth", "login", "authorization", "bearer",
                "authinterceptor", "handlerinterceptor", "bcrypt", "登录", "鉴权"
        ));
        keywords.put(ClaimCategory.DATABASE, List.of(
                "mysql", "sql", "mapper", "entity", "repository", "datasource", "transactional", "数据库"
        ));
        keywords.put(ClaimCategory.CACHE, List.of(
                "redis", "redistemplate", "stringredistemplate", "cache", "缓存", "taskprogress"
        ));
        keywords.put(ClaimCategory.AI, List.of(
                "llm", "openai", "deepseek", "chatcompletion", "prompt", "aicall", "大模型", "幻觉"
        ));
        keywords.put(ClaimCategory.RAG_OR_QA, List.of(
                "projectqa", "question", "evidence", "retrieval", "keyword", "topk",
                "embedding", "vector", "问答", "检索"
        ));
        keywords.put(ClaimCategory.FILE_UPLOAD, List.of(
                "upload", "zip", "multipart", "projectfile", "zipfileutil", "readme", "文件", "上传"
        ));
        keywords.put(ClaimCategory.REPORT, List.of(
                "analysisreport", "report", "scanner", "evidencechain", "resume", "审计", "报告", "证据"
        ));
        keywords.put(ClaimCategory.INTERVIEW, List.of(
                "interview", "session", "question", "finish", "review", "面试", "复盘"
        ));
        keywords.put(ClaimCategory.ADMIN, List.of(
                "admin", "admininterceptor", "adminemails", "/api/admin", "feedback", "管理员", "后台"
        ));
        keywords.put(ClaimCategory.CREDIT, List.of(
                "credit", "consumecredits", "refundcredits", "transaction", "额度", "扣减", "返还"
        ));
        keywords.put(ClaimCategory.DEPLOYMENT, List.of(
                "docker", "docker-compose", "nginx", "deploy", "backup", "restore", "部署", "备份", "恢复"
        ));
        keywords.put(ClaimCategory.FRONTEND, List.of(
                "vue", "router", "pinia", "axios", "dashboard", "i18n", "element-plus", "前端", "页面"
        ));
        keywords.put(ClaimCategory.SECURITY, List.of(
                "security", "interceptor", "secret", "password", "sanitize", "cors", "敏感", "安全", "隔离"
        ));
        keywords.put(ClaimCategory.PERFORMANCE, List.of(
                "jmeter", "wrk", "benchmark", "qps", "ratelimiter", "sentinel", "threadpool", "压测", "限流"
        ));
        keywords.put(ClaimCategory.BUSINESS_OR_PRODUCT, List.of(
                "feedback", "dashboard", "user", "payment", "order", "metrics", "用户", "反馈", "支付", "统计"
        ));
        keywords.put(ClaimCategory.GENERAL, List.of());
        return keywords;
    }

    private boolean containsAny(String text, List<String> keywords) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record EvidenceMatch(ProjectFile file,
                                 boolean strong,
                                 Integer score,
                                 List<String> matchedKeywords,
                                 String snippet) {
    }

    private record IndexedFile(ProjectFile file,
                               String pathLower,
                               String content,
                               String contentLower,
                               boolean strong) {
    }
}
