package com.xinzhe.projectmentor.claim;

import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.project.entity.Project;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimEvidenceEngineTests {

    private final ClaimTextSanitizer sanitizer = new ClaimTextSanitizer();

    private final ClaimExtractor extractor = new ClaimExtractor(sanitizer);

    private final ClaimEvidenceMatcher matcher = new ClaimEvidenceMatcher(sanitizer);

    @Test
    void extractsClaimsFromDescriptionTechStackAndReadme() {
        Project project = new Project();
        project.setDescription("实现 JWT 登录鉴权；支持管理员后台和反馈管理。");
        project.setTechStack("Redis, MySQL");

        ProjectFile readme = file(
                1L,
                "README.md",
                "README",
                "项目支持 Docker Compose 部署，并提供数据库备份和恢复脚本。"
        );

        List<ClaimEvidenceItemVO> claims = extractor.extract(project, List.of(readme));

        assertThat(claims)
                .extracting(ClaimEvidenceItemVO::getSourceType)
                .contains("PROJECT_DESCRIPTION", "TECH_STACK", "README");
        assertThat(claims)
                .extracting(ClaimEvidenceItemVO::getClaimText)
                .anyMatch(text -> text.contains("JWT"))
                .anyMatch(text -> text.contains("Redis"))
                .anyMatch(text -> text.contains("Docker"));
    }

    @Test
    void marksJwtClaimSupportedWhenMultipleStrongFilesMatch() {
        ClaimEvidenceItemVO claim = ClaimEvidenceItemVO.builder()
                .claimText("项目实现 JWT 登录鉴权和请求拦截")
                .sourceType("PROJECT_DESCRIPTION")
                .sourceSnippet("项目实现 JWT 登录鉴权和请求拦截")
                .category(ClaimCategory.AUTH)
                .build();

        ProjectFile jwtUtil = file(
                1L,
                "backend/src/main/java/example/JwtUtil.java",
                "CODE",
                "class JwtUtil { String createToken() { return \"jwt\"; } }"
        );
        ProjectFile interceptor = file(
                2L,
                "backend/src/main/java/example/AuthInterceptor.java",
                "CODE",
                "String authorization = request.getHeader(\"Authorization\"); // Bearer token"
        );

        ClaimEvidenceItemVO result = matcher.match(claim, List.of(jwtUtil, interceptor));

        assertThat(result.getStatus()).isEqualTo(ClaimEvidenceStatus.SUPPORTED);
        assertThat(result.getEvidenceFiles()).hasSize(2);
        assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void marksUnsupportedPerformanceClaimRisky() {
        ClaimEvidenceItemVO claim = ClaimEvidenceItemVO.builder()
                .claimText("系统达到企业级高并发和百万用户规模")
                .sourceType("README")
                .sourceSnippet("系统达到企业级高并发和百万用户规模")
                .category(ClaimCategory.PERFORMANCE)
                .build();

        ProjectFile readme = file(
                1L,
                "README.md",
                "README",
                "系统达到企业级高并发和百万用户规模"
        );

        ClaimEvidenceItemVO result = matcher.match(claim, List.of(readme));

        assertThat(result.getStatus()).isEqualTo(ClaimEvidenceStatus.RISKY);
    }

    @Test
    void sanitizesSecretsAndHonorsSnippetLimit() {
        String content = "AI_API_KEY=sk-secret-value JWT_SECRET: local-secret "
                + "Authorization: Bearer abcdefghijklmnopqrstuvwxyz "
                + "x".repeat(500);

        String sanitized = sanitizer.sanitizeAndLimit(content, 300);

        assertThat(sanitized)
                .doesNotContain("sk-secret-value")
                .doesNotContain("local-secret")
                .doesNotContain("abcdefghijklmnopqrstuvwxyz")
                .contains("AI_API_KEY=******")
                .hasSizeLessThanOrEqualTo(300);
    }

    private ProjectFile file(Long id, String path, String type, String content) {
        ProjectFile file = new ProjectFile();
        file.setId(id);
        file.setProjectId(10L);
        file.setFilePath(path);
        file.setFileType(type);
        file.setContent(content);
        return file;
    }
}
