package com.xinzhe.projectmentor.scanner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.scanner.vo.EvidenceItemVO;
import com.xinzhe.projectmentor.scanner.vo.RiskItemVO;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectRuleScanner {

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

    private final ReadmeRiskScanner readmeRiskScanner;

    public RuleScanResultVO scanProject(Long projectId) {
        Project project = checkProjectOwner(projectId);

        List<ProjectFile> files = projectFileMapper.selectList(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
        );

        List<RiskItemVO> risks = new ArrayList<>();
        List<EvidenceItemVO> evidences = new ArrayList<>();

        Optional<ProjectFile> readmeOptional = findReadme(files);

        if (files.isEmpty()) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("PROJECT_FILE_MISSING")
                    .keyword("项目文件")
                    .sourceFile("-")
                    .message("当前项目下没有任何已保存文件，无法进行有效项目审计。")
                    .evidence("pm_project_file 中未查询到该项目的文件记录")
                    .suggestion("请先粘贴 README，后续再上传 pom.xml、配置文件和核心代码。")
                    .build());
        }

        if (readmeOptional.isPresent()) {
            ProjectFile readme = readmeOptional.get();

            evidences.add(EvidenceItemVO.builder()
                    .conclusion("项目存在 README 文件")
                    .sourceFile(readme.getFilePath())
                    .detail("已发现 README 内容，长度为 " + safeLength(readme.getContent()) + " 个字符")
                    .riskLevel("INFO")
                    .build());

            risks.addAll(readmeRiskScanner.scan(readme.getContent(), files));
        } else {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("README_MISSING")
                    .keyword("README")
                    .sourceFile("-")
                    .message("当前项目未发现 README.md，无法判断项目描述、技术栈和运行方式。")
                    .evidence("项目文件列表中未发现 file_path = README.md 或 file_type = README 的文件")
                    .suggestion("请先粘贴 README 内容，至少说明项目背景、技术栈、启动方式和已实现功能。")
                    .build());
        }

        buildFileEvidence(files, evidences);

        int highRiskCount = countRiskByLevel(risks, "HIGH");
        int mediumRiskCount = countRiskByLevel(risks, "MEDIUM");
        int lowRiskCount = countRiskByLevel(risks, "LOW");

        return RuleScanResultVO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .hasReadme(readmeOptional.isPresent())
                .fileCount(files.size())
                .totalRiskCount(risks.size())
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .risks(risks)
                .evidences(evidences)
                .suggestions(buildSuggestions(risks, readmeOptional.isPresent()))
                .build();
    }

    private Project checkProjectOwner(Long projectId) {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限扫描");
        }

        return project;
    }

    private Optional<ProjectFile> findReadme(List<ProjectFile> files) {
        return files.stream()
                .filter(file -> {
                    String filePath = file.getFilePath();
                    String fileType = file.getFileType();

                    return "README.md".equalsIgnoreCase(filePath)
                            || "README".equalsIgnoreCase(fileType);
                })
                .findFirst();
    }

    private void buildFileEvidence(List<ProjectFile> files, List<EvidenceItemVO> evidences) {
        if (files == null || files.isEmpty()) {
            return;
        }

        if (hasFilePath(files, "pom.xml")) {
            evidences.add(info("项目存在 Maven 配置文件", "pom.xml", "已发现 pom.xml，可用于判断 Java 技术栈和依赖。"));
        }

        if (hasFilePath(files, "dockerfile")) {
            evidences.add(info("项目存在 Dockerfile", "Dockerfile", "已发现 Dockerfile，可作为 Docker 部署证据。"));
        }

        if (hasFilePath(files, "docker-compose.yml") || hasFilePath(files, "docker-compose.yaml")) {
            evidences.add(info("项目存在 docker-compose 配置", "docker-compose.yml", "已发现 docker-compose 文件，可作为环境编排证据。"));
        }

        if (hasFilePath(files, "application.yml") || hasFilePath(files, "application.properties")) {
            evidences.add(info("项目存在 Spring Boot 配置文件", "application.yml / application.properties", "已发现应用配置文件，可用于检查数据库、Redis、AI Key 等配置风险。"));
        }

        if (hasFilePath(files, ".gitignore")) {
            evidences.add(info("项目存在 .gitignore", ".gitignore", "已发现 .gitignore，可用于判断是否忽略敏感配置和构建产物。"));
        }

        if (hasFilePathContains(files, ".sql")) {
            evidences.add(info("项目存在 SQL 初始化脚本", "SQL 文件", "已发现 SQL 文件，可作为数据库初始化能力证据。"));
        }

        String allText = buildAllText(files);

        if (allText.contains("mybatis-plus")) {
            evidences.add(info("项目使用 MyBatis-Plus", "项目文件内容", "已发现 mybatis-plus 相关依赖或配置。"));
        }

        if (allText.contains("redistemplate") || allText.contains("stringredistemplate")) {
            evidences.add(info("项目存在 Redis 使用证据", "项目文件内容", "已发现 RedisTemplate 或 StringRedisTemplate。"));
        }

        if (allText.contains("jwtutil") || allText.contains("jsonwebtoken")) {
            evidences.add(info("项目存在 JWT 使用证据", "项目文件内容", "已发现 JwtUtil 或 jsonwebtoken 相关内容。"));
        }

        if (allText.contains("globalexceptionhandler")) {
            evidences.add(info("项目存在全局异常处理", "项目文件内容", "已发现 GlobalExceptionHandler。"));
        }
    }

    private EvidenceItemVO info(String conclusion, String sourceFile, String detail) {
        return EvidenceItemVO.builder()
                .conclusion(conclusion)
                .sourceFile(sourceFile)
                .detail(detail)
                .riskLevel("INFO")
                .build();
    }

    private boolean hasFilePath(List<ProjectFile> files, String target) {
        String targetLower = target.toLowerCase(Locale.ROOT);

        return files.stream()
                .map(ProjectFile::getFilePath)
                .filter(path -> path != null)
                .map(path -> path.toLowerCase(Locale.ROOT))
                .anyMatch(path -> path.endsWith(targetLower) || path.equals(targetLower));
    }

    private boolean hasFilePathContains(List<ProjectFile> files, String target) {
        String targetLower = target.toLowerCase(Locale.ROOT);

        return files.stream()
                .map(ProjectFile::getFilePath)
                .filter(path -> path != null)
                .map(path -> path.toLowerCase(Locale.ROOT))
                .anyMatch(path -> path.contains(targetLower));
    }

    private String buildAllText(List<ProjectFile> files) {
        StringBuilder builder = new StringBuilder();

        for (ProjectFile file : files) {
            if (file.getFilePath() != null) {
                builder.append(file.getFilePath()).append("\n");
            }
            if (file.getContent() != null) {
                builder.append(file.getContent()).append("\n");
            }
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private int countRiskByLevel(List<RiskItemVO> risks, String level) {
        return (int) risks.stream()
                .filter(risk -> level.equalsIgnoreCase(risk.getRiskLevel()))
                .count();
    }

    private int safeLength(String content) {
        return content == null ? 0 : content.length();
    }

    private List<String> buildSuggestions(List<RiskItemVO> risks, boolean hasReadme) {
        List<String> suggestions = new ArrayList<>();

        if (!hasReadme) {
            suggestions.add("优先补充 README.md，说明项目背景、技术栈、启动方式、已实现功能和未实现功能。");
        }

        boolean hasHighRisk = risks.stream()
                .anyMatch(risk -> "HIGH".equalsIgnoreCase(risk.getRiskLevel()));

        if (hasHighRisk) {
            suggestions.add("优先处理 HIGH 级风险，删除或改写没有证据支撑的高并发、微服务、RAG 等表述。");
        }

        boolean hasMissingEvidence = risks.stream()
                .anyMatch(risk -> "MISSING_TECH_EVIDENCE".equalsIgnoreCase(risk.getRiskType()));

        if (hasMissingEvidence) {
            suggestions.add("如果 README 中提到了 Redis、JWT、Docker、RAG 等技术，请上传对应配置文件或核心代码作为证据。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("当前 README 风险较少，后续可以继续上传 pom.xml、配置文件和核心代码，以便进行更完整的项目真实性审计。");
        }

        return suggestions;
    }
}