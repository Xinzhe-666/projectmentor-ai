package com.xinzhe.projectmentor.claim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceFileVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClaimEvidenceAiPromptBuilder {

    private static final int MAX_CLAIMS = 15;

    private static final int MAX_EVIDENCE_FILES = 5;

    private static final int MAX_SNIPPET_CHARS = 300;

    private static final String SYSTEM_PROMPT = """
            你是项目真实性审计助手。你不能鼓励用户夸大项目。
            你只能基于给定 claim、status、reason、evidenceFiles、resumeAdvice 和 interviewQuestion 解释。
            对 DOC_ONLY、NO_EVIDENCE、RISKY 的 claim，必须提醒用户保守表达。
            对 SUPPORTED 的 claim，可以建议更清晰但仍然基于证据的表达。
            不要编造未提供的代码、性能数据、用户量、QPS、商业成果或生产级结论。
            只输出 JSON，不要输出 Markdown 或额外解释。
            """;

    private final ObjectMapper objectMapper;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String build(List<ClaimEvidenceItemVO> claims) {
        List<Map<String, Object>> promptClaims = claims.stream()
                .sorted(Comparator
                        .comparingInt((ClaimEvidenceItemVO claim) -> statusPriority(claim.getStatus()))
                        .thenComparingInt(claim -> claim.getConfidenceScore() == null ? 0 : claim.getConfidenceScore()))
                .limit(MAX_CLAIMS)
                .map(this::toPromptClaim)
                .toList();

        String claimsJson;
        try {
            claimsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptClaims);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "构建 AI Claim-Evidence 提示失败");
        }

        return """
                请基于以下已经结构化、已脱敏的 Claim-Evidence 数据，生成更自然的深度解读。
                不要重新扫描仓库，不要要求更多源码，不要输出未给定的实现细节。

                【待分析 Claim-Evidence 数据】
                %s

                【输出要求】
                严格输出一个 JSON object，字段如下：
                {
                  "summary": "整体主张证据审计总结",
                  "riskOverview": "主要风险概览",
                  "resumeStrategy": "简历表达策略",
                  "interviewStrategy": "面试准备策略",
                  "items": [
                    {
                      "claimText": "必须与输入 claimText 对应",
                      "aiExplanation": "为什么当前证据状态合理",
                      "saferResumeExpression": "更稳妥的简历表达",
                      "likelyInterviewQuestions": ["真实、具体的追问"],
                      "improvementSuggestion": "如何补强证据"
                    }
                  ]
                }
                """.formatted(claimsJson);
    }

    private Map<String, Object> toPromptClaim(ClaimEvidenceItemVO claim) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("claimText", safe(claim.getClaimText()));
        item.put("status", claim.getStatus() == null ? "" : claim.getStatus().name());
        item.put("category", claim.getCategory() == null ? "" : claim.getCategory().name());
        item.put("reason", safe(claim.getReason()));
        item.put("resumeAdvice", safe(claim.getResumeAdvice()));
        item.put("interviewQuestion", safe(claim.getInterviewQuestion()));
        item.put("evidenceFiles", (claim.getEvidenceFiles() == null ? List.<ClaimEvidenceFileVO>of() : claim.getEvidenceFiles())
                .stream()
                .limit(MAX_EVIDENCE_FILES)
                .map(this::toPromptEvidence)
                .toList());
        return item;
    }

    private Map<String, Object> toPromptEvidence(ClaimEvidenceFileVO evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("filePath", safe(evidence.getFilePath()));
        item.put("evidenceLevel", safe(evidence.getEvidenceLevel()));
        item.put("matchedKeywords", evidence.getMatchedKeywords() == null
                ? List.of()
                : evidence.getMatchedKeywords().stream().limit(8).toList());
        item.put("snippet", truncate(safe(evidence.getSnippet()), MAX_SNIPPET_CHARS));
        return item;
    }

    private int statusPriority(ClaimEvidenceStatus status) {
        if (status == null) {
            return 99;
        }

        return switch (status) {
            case RISKY -> 0;
            case NO_EVIDENCE -> 1;
            case DOC_ONLY -> 2;
            case PARTIAL -> 3;
            case SUPPORTED -> 4;
        };
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }

        return text.substring(0, maxChars);
    }
}
