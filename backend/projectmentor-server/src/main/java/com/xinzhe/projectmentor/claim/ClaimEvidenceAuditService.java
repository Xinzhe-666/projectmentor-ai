package com.xinzhe.projectmentor.claim;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceAiEnhancementVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceAiItemVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimEvidenceAuditService {

    private static final TypeReference<List<ClaimEvidenceItemVO>> CLAIM_LIST_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<List<ClaimEvidenceAiItemVO>> AI_ITEM_LIST_TYPE = new TypeReference<>() {
    };

    private final ProjectFileMapper projectFileMapper;

    private final ClaimExtractor claimExtractor;

    private final ClaimEvidenceMatcher claimEvidenceMatcher;

    private final ObjectMapper objectMapper;

    public ClaimEvidenceVO audit(Project project, Long projectId) {
        checkProjectOwner(project, projectId);

        List<ProjectFile> files = projectFileMapper.selectList(
                new LambdaQueryWrapper<ProjectFile>()
                        .select(
                                ProjectFile::getId,
                                ProjectFile::getProjectId,
                                ProjectFile::getFilePath,
                                ProjectFile::getFileType,
                                ProjectFile::getContent
                        )
                        .eq(ProjectFile::getProjectId, projectId)
        );

        List<ClaimEvidenceItemVO> extractedClaims = claimExtractor.extract(project, files);
        List<ClaimEvidenceItemVO> items = claimEvidenceMatcher.matchAll(extractedClaims, files);

        return ClaimEvidenceVO.builder()
                .projectId(projectId)
                .totalClaims(items.size())
                .statusCounts(countStatuses(items))
                .items(items)
                .build();
    }

    public List<ClaimEvidenceItemVO> parseItems(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode itemsNode = root.isArray() ? root : root.get("items");

            if (itemsNode == null || !itemsNode.isArray()) {
                return Collections.emptyList();
            }

            List<ClaimEvidenceItemVO> items = objectMapper.convertValue(itemsNode, CLAIM_LIST_TYPE);
            return items == null ? Collections.emptyList() : items;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public ClaimEvidenceAiEnhancementVO parseAiEnhancement(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return null;
            }

            List<ClaimEvidenceAiItemVO> aiItems = parseAiItems(root.get("aiEnhancedItems"));
            String aiSummary = text(root, "aiSummary");
            String aiRiskOverview = text(root, "aiRiskOverview");
            String aiResumeStrategy = text(root, "aiResumeStrategy");
            String aiInterviewStrategy = text(root, "aiInterviewStrategy");
            String aiFallbackText = text(root, "aiFallbackText");

            boolean hasEnhancement = root.path("aiEnhanced").asBoolean(false)
                    || StringUtils.hasText(aiSummary)
                    || StringUtils.hasText(aiRiskOverview)
                    || StringUtils.hasText(aiResumeStrategy)
                    || StringUtils.hasText(aiInterviewStrategy)
                    || StringUtils.hasText(aiFallbackText)
                    || !aiItems.isEmpty();

            if (!hasEnhancement) {
                return null;
            }

            return ClaimEvidenceAiEnhancementVO.builder()
                    .aiEnhanced(root.path("aiEnhanced").asBoolean(true))
                    .aiEnhancedAt(parseDateTime(text(root, "aiEnhancedAt")))
                    .aiSummary(aiSummary)
                    .aiRiskOverview(aiRiskOverview)
                    .aiResumeStrategy(aiResumeStrategy)
                    .aiInterviewStrategy(aiInterviewStrategy)
                    .aiEnhancedItems(aiItems)
                    .aiFallbackText(aiFallbackText)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void checkProjectOwner(Project project, Long projectId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (project == null
                || project.getId() == null
                || !project.getId().equals(projectId)
                || project.getUserId() == null
                || !project.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限执行主张证据审计");
        }
    }

    private Map<String, Integer> countStatuses(List<ClaimEvidenceItemVO> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ClaimEvidenceStatus status : ClaimEvidenceStatus.values()) {
            counts.put(status.name(), 0);
        }

        for (ClaimEvidenceItemVO item : items) {
            if (item.getStatus() != null) {
                counts.computeIfPresent(item.getStatus().name(), (key, value) -> value + 1);
            }
        }

        return counts;
    }

    private List<ClaimEvidenceAiItemVO> parseAiItems(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }

        try {
            List<ClaimEvidenceAiItemVO> items = objectMapper.convertValue(node, AI_ITEM_LIST_TYPE);
            return items == null ? Collections.emptyList() : items;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? "" : node.asText("");
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
