package com.xinzhe.projectmentor.claim;

import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ClaimExtractor {

    private static final int MAX_CLAIMS = 30;
    private static final int MIN_CLAIM_LENGTH = 8;
    private static final int MAX_CLAIM_LENGTH = 180;

    private static final Pattern SPLIT_PATTERN = Pattern.compile(
            "[\\r\\n]+|[。！？!?；;：:]+"
    );

    private static final Pattern MARKDOWN_PREFIX_PATTERN = Pattern.compile(
            "^\\s*(?:#{1,6}\\s*|[-*+>]\\s*|\\d+[.)、]\\s*|\\[[ xX]]\\s*)+"
    );

    private static final List<String> PRIORITY_KEYWORDS = List.of(
            "jwt", "redis", "mysql", "docker", "nginx", "ai", "rag", "sse", "异步",
            "权限", "管理员", "额度", "反馈", "上传", "报告", "面试", "国际化", "部署",
            "备份", "恢复", "上线", "用户", "后台", "统计", "安全", "鉴权", "限流",
            "缓存", "事务", "日志", "高并发", "企业级", "分布式", "微服务", "大规模",
            "高可用", "生产级", "商业化", "qps", "百万用户", "完整支付", "真实支付"
    );

    private static final Set<String> GENERIC_PHRASES = Set.of(
            "项目介绍", "项目简介", "核心功能", "功能介绍", "技术栈", "主要功能",
            "系统介绍", "关于项目", "项目背景", "功能列表", "已完成", "当前版本"
    );

    private final ClaimTextSanitizer sanitizer;

    public List<ClaimEvidenceItemVO> extract(Project project, List<ProjectFile> files) {
        Map<String, ClaimCandidate> candidates = new LinkedHashMap<>();

        addTextCandidates(candidates, project.getDescription(), "PROJECT_DESCRIPTION", true);
        addTechStackCandidates(candidates, project.getTechStack());

        files.stream()
                .filter(this::isReadme)
                .map(ProjectFile::getContent)
                .filter(content -> content != null && !content.isBlank())
                .forEach(content -> addTextCandidates(candidates, content, "README", false));

        return candidates.values().stream()
                .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
                .limit(MAX_CLAIMS)
                .map(candidate -> ClaimEvidenceItemVO.builder()
                        .claimText(candidate.text())
                        .sourceType(candidate.sourceType())
                        .sourceSnippet(candidate.sourceSnippet())
                        .category(classify(candidate.text()))
                        .evidenceFiles(List.of())
                        .build())
                .toList();
    }

    private void addTechStackCandidates(Map<String, ClaimCandidate> candidates, String techStack) {
        if (techStack == null || techStack.isBlank()) {
            return;
        }

        String sanitized = sanitizer.sanitizeAndLimit(techStack, MAX_CLAIM_LENGTH);
        String[] parts = sanitized.split("[,，/|、]+");
        List<String> normalizedParts = new ArrayList<>();

        for (String part : parts) {
            String normalized = cleanCandidate(part);
            if (normalized.length() >= 2 && normalized.length() <= 80) {
                normalizedParts.add(normalized);
            }
        }

        if (normalizedParts.isEmpty()) {
            addCandidate(candidates, "项目技术栈包含 " + sanitized, "TECH_STACK", sanitized, 3);
            return;
        }

        for (String part : normalizedParts) {
            addCandidate(candidates, "项目技术栈包含 " + part, "TECH_STACK", sanitized, 3);
        }
    }

    private void addTextCandidates(Map<String, ClaimCandidate> candidates,
                                   String source,
                                   String sourceType,
                                   boolean acceptGeneral) {
        if (source == null || source.isBlank()) {
            return;
        }

        for (String rawPart : SPLIT_PATTERN.split(source)) {
            String cleaned = cleanCandidate(rawPart);
            if (!isUsable(cleaned)) {
                continue;
            }

            boolean priority = containsPriorityKeyword(cleaned);
            if (!priority && !acceptGeneral) {
                continue;
            }

            int score = priority ? 3 : 1;
            if ("PROJECT_DESCRIPTION".equals(sourceType)) {
                score++;
            }
            if (containsRiskKeyword(cleaned)) {
                score += 2;
            }

            addCandidate(
                    candidates,
                    cleaned,
                    sourceType,
                    sanitizer.sanitizeAndLimit(rawPart, 220),
                    score
            );
        }
    }

    private void addCandidate(Map<String, ClaimCandidate> candidates,
                              String text,
                              String sourceType,
                              String sourceSnippet,
                              int priority) {
        String safeText = sanitizer.sanitizeAndLimit(text, MAX_CLAIM_LENGTH);
        if (!isUsable(safeText)) {
            return;
        }

        String key = safeText.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\s]+", "");

        ClaimCandidate existing = candidates.get(key);
        if (existing == null || existing.priority() < priority) {
            candidates.put(key, new ClaimCandidate(
                    safeText,
                    sourceType,
                    sanitizer.sanitizeAndLimit(sourceSnippet, 220),
                    priority
            ));
        }
    }

    private String cleanCandidate(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = MARKDOWN_PREFIX_PATTERN.matcher(raw).replaceFirst("");
        cleaned = cleaned
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replace("`", "")
                .replaceAll("\\s+", " ")
                .trim();

        return sanitizer.sanitizeAndLimit(cleaned, MAX_CLAIM_LENGTH);
    }

    private boolean isUsable(String candidate) {
        if (candidate == null
                || candidate.length() < MIN_CLAIM_LENGTH
                || candidate.length() > MAX_CLAIM_LENGTH) {
            return false;
        }

        String normalized = candidate.toLowerCase(Locale.ROOT).trim();
        if (GENERIC_PHRASES.contains(normalized)) {
            return false;
        }

        long letterOrDigitCount = normalized.codePoints()
                .filter(Character::isLetterOrDigit)
                .count();
        return letterOrDigitCount >= 5;
    }

    private boolean containsPriorityKeyword(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return PRIORITY_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean containsRiskKeyword(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return List.of(
                "高并发", "企业级", "生产级", "分布式", "微服务", "高可用", "百万用户",
                "qps", "商业化", "完整支付", "真实支付", "海量数据", "大规模"
        ).stream().anyMatch(lower::contains);
    }

    private boolean isReadme(ProjectFile file) {
        String filePath = file.getFilePath() == null ? "" : file.getFilePath().toLowerCase(Locale.ROOT);
        String fileType = file.getFileType() == null ? "" : file.getFileType();
        return "README".equalsIgnoreCase(fileType)
                || filePath.equals("readme.md")
                || filePath.endsWith("/readme.md")
                || filePath.endsWith("\\readme.md");
    }

    private ClaimCategory classify(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "高并发", "qps", "百万用户", "海量数据", "大规模", "性能", "压测")) {
            return ClaimCategory.PERFORMANCE;
        }
        if (containsAny(lower, "jwt", "登录", "认证", "鉴权", "authorization", "bcrypt", "权限")) {
            return ClaimCategory.AUTH;
        }
        if (containsAny(lower, "mysql", "数据库", "sql", "mapper", "数据表", "事务")) {
            return ClaimCategory.DATABASE;
        }
        if (containsAny(lower, "redis", "缓存", "任务进度")) {
            return ClaimCategory.CACHE;
        }
        if (containsAny(lower, "rag", "问答", "检索", "向量", "embedding", "知识库")) {
            return ClaimCategory.RAG_OR_QA;
        }
        if (containsAny(lower, "ai", "llm", "deepseek", "openai", "大模型", "幻觉")) {
            return ClaimCategory.AI;
        }
        if (containsAny(lower, "上传", "zip", "readme", "文件解析")) {
            return ClaimCategory.FILE_UPLOAD;
        }
        if (containsAny(lower, "审计报告", "报告生成", "简历建议", "报告历史", "证据链")) {
            return ClaimCategory.REPORT;
        }
        if (containsAny(lower, "面试", "追问", "复盘")) {
            return ClaimCategory.INTERVIEW;
        }
        if (containsAny(lower, "管理员", "后台管理", "反馈管理", "admin")) {
            return ClaimCategory.ADMIN;
        }
        if (containsAny(lower, "额度", "积分", "扣减", "返还", "credit")) {
            return ClaimCategory.CREDIT;
        }
        if (containsAny(lower, "docker", "nginx", "部署", "上线", "备份", "恢复", "容器")) {
            return ClaimCategory.DEPLOYMENT;
        }
        if (containsAny(lower, "vue", "前端", "dashboard", "国际化", "i18n", "页面", "ui")) {
            return ClaimCategory.FRONTEND;
        }
        if (containsAny(lower, "安全", "敏感", "密钥", "限流", "隔离", "拦截")) {
            return ClaimCategory.SECURITY;
        }
        if (containsAny(lower, "用户", "反馈", "产品", "试用", "商业化", "支付", "闭环")) {
            return ClaimCategory.BUSINESS_OR_PRODUCT;
        }

        return ClaimCategory.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record ClaimCandidate(String text, String sourceType, String sourceSnippet, int priority) {
    }
}
