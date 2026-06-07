package com.xinzhe.projectmentor.claim;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimEvidenceAuditService {

    private static final TypeReference<List<ClaimEvidenceItemVO>> CLAIM_LIST_TYPE = new TypeReference<>() {
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
            List<ClaimEvidenceItemVO> items = objectMapper.readValue(json, CLAIM_LIST_TYPE);
            return items == null ? Collections.emptyList() : items;
        } catch (Exception ignored) {
            return Collections.emptyList();
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
}
