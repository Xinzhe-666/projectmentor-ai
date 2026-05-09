package com.xinzhe.projectmentor.scanner;

import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.scanner.vo.RiskItemVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ReadmeRiskScanner {

    public List<RiskItemVO> scan(String readmeContent, List<ProjectFile> allFiles) {
        List<RiskItemVO> risks = new ArrayList<>();

        if (readmeContent == null || readmeContent.isBlank()) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("README_MISSING")
                    .keyword("README")
                    .sourceFile("README.md")
                    .message("项目 README 内容为空，无法判断项目功能、技术栈和运行方式。")
                    .evidence("未发现有效 README 内容")
                    .suggestion("补充项目背景、技术栈、启动方式、核心功能和真实实现范围。")
                    .build());
            return risks;
        }

        String readmeLower = readmeContent.toLowerCase(Locale.ROOT);
        String evidenceText = buildEvidenceText(allFiles);

        checkHighConcurrencyClaim(readmeLower, evidenceText, risks);
        checkMicroserviceClaim(readmeLower, evidenceText, risks);
        checkEnterpriseClaim(readmeLower, evidenceText, risks);
        checkRagClaim(readmeLower, evidenceText, risks);
        checkDockerClaim(readmeLower, evidenceText, risks);
        checkRedisClaim(readmeLower, evidenceText, risks);
        checkJwtClaim(readmeLower, evidenceText, risks);

        return risks;
    }

    private void checkHighConcurrencyClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("高并发", "1000qps", "千万级", "大规模", "秒杀"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "jmeter", "wrk", "压测", "限流", "ratelimiter", "sentinel",
                "redis", "redistemplate", "@async", "threadpoolexecutor", "线程池"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("README_OVERCLAIM")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中出现“" + matched + "”等高并发相关表述，但当前已上传文件中未发现压测、限流、缓存、异步或线程池等实现证据。")
                    .evidence("未发现 JMeter/wrk 压测报告、Sentinel/RateLimiter 限流、Redis 缓存、@Async 或线程池相关代码")
                    .suggestion("如果没有真实实现和测试数据，建议删除高并发表述，改为“具备基础接口设计”或“预留性能优化空间”。")
                    .build());
        }
    }

    private void checkMicroserviceClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("微服务", "spring cloud", "nacos", "dubbo", "gateway", "openfeign"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "spring-cloud", "nacos", "dubbo", "gateway", "openfeign", "eureka", "loadbalancer"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("README_OVERCLAIM")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中出现“" + matched + "”等微服务相关表述，但当前已上传文件中未发现注册中心、网关、远程调用或多服务模块证据。")
                    .evidence("未发现 Spring Cloud、Nacos、Gateway、OpenFeign、Dubbo 或多模块服务结构")
                    .suggestion("如果项目是单体架构，建议明确写成“模块化单体架构”，不要写“微服务架构”。")
                    .build());
        }
    }

    private void checkEnterpriseClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("企业级", "生产级", "完整闭环", "核心系统", "高可用", "99.99", "云原生", "gdpr"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "globalexceptionhandler", "统一返回", "result", "dockerfile", "docker-compose",
                "knife4j", "swagger", "jwtutil", "init.sql", "日志", "监控"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("MEDIUM")
                    .riskType("README_OVERCLAIM")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中出现“" + matched + "”等偏包装化表述，但当前已上传文件中缺少足够工程化证据支撑。")
                    .evidence("未发现完整的异常处理、统一返回、接口文档、Docker 部署、初始化 SQL、日志或监控等证据")
                    .suggestion("建议改成更具体、可证明的表述，例如“实现统一返回、全局异常处理和接口文档”。")
                    .build());
        }
    }

    private void checkRagClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("rag", "向量检索", "embedding", "知识库问答", "相似度检索"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "embedding", "chunk", "vector", "topk", "similarity", "cosine", "milvus", "pgvector", "向量"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("HIGH")
                    .riskType("MISSING_TECH_EVIDENCE")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中提到“" + matched + "”，但当前已上传文件中未发现切块、向量化、相似度检索或 Top-K 检索证据。")
                    .evidence("未发现 chunk、embedding、vector、similarity、cosine、Milvus、PgVector 等相关实现")
                    .suggestion("如果只是调用大模型 API，不建议写 RAG；如果确实实现了 RAG，请上传相关核心代码作为证据。")
                    .build());
        }
    }

    private void checkDockerClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("docker", "docker-compose", "容器化"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "dockerfile", "docker-compose.yml", "docker-compose.yaml"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("MEDIUM")
                    .riskType("MISSING_TECH_EVIDENCE")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中提到 Docker 或容器化部署，但当前已上传文件中未发现 Dockerfile 或 docker-compose.yml。")
                    .evidence("未发现 Dockerfile / docker-compose.yml / docker-compose.yaml")
                    .suggestion("如果已经支持 Docker，请上传 Dockerfile 和 docker-compose.yml；如果没有实现，建议删除该表述。")
                    .build());
        }
    }

    private void checkRedisClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("redis", "缓存"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "redistemplate", "stringredistemplate", "spring.redis", "spring.data.redis", "lettuce", "jedis"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("MEDIUM")
                    .riskType("MISSING_TECH_EVIDENCE")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中提到 Redis 或缓存，但当前已上传文件中未发现 RedisTemplate、StringRedisTemplate 或 Redis 配置证据。")
                    .evidence("未发现 RedisTemplate / StringRedisTemplate / spring.data.redis / Lettuce / Jedis")
                    .suggestion("如果 Redis 只是计划功能，不建议写成已实现；如果已实现，请上传配置和使用 Redis 的核心代码。")
                    .build());
        }
    }

    private void checkJwtClaim(String readmeLower, String evidenceText, List<RiskItemVO> risks) {
        String matched = firstMatched(readmeLower, List.of("jwt", "token", "登录认证"));

        if (matched == null) {
            return;
        }

        boolean hasEvidence = containsAny(evidenceText, List.of(
                "jwtutil", "jsonwebtoken", "authorization", "bearer", "handlerinterceptor", "拦截器"
        ));

        if (!hasEvidence) {
            risks.add(RiskItemVO.builder()
                    .riskLevel("MEDIUM")
                    .riskType("MISSING_TECH_EVIDENCE")
                    .keyword(matched)
                    .sourceFile("README.md")
                    .message("README 中提到 JWT 或登录认证，但当前已上传文件中未发现 JwtUtil、Authorization、Bearer Token 或拦截器相关证据。")
                    .evidence("未发现 JwtUtil / jsonwebtoken / Authorization / Bearer / HandlerInterceptor")
                    .suggestion("如果已经实现 JWT，请上传 JwtUtil、登录接口和拦截器代码；如果没有实现，建议改成“计划支持登录认证”。")
                    .build());
        }
    }

    private String buildEvidenceText(List<ProjectFile> allFiles) {
        if (allFiles == null || allFiles.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (ProjectFile file : allFiles) {
            if (file.getFilePath() != null && "README.md".equalsIgnoreCase(file.getFilePath())) {
                continue;
            }

            if (file.getFilePath() != null) {
                builder.append(file.getFilePath()).append("\n");
            }

            if (file.getContent() != null) {
                builder.append(file.getContent()).append("\n");
            }
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private String firstMatched(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return keyword;
            }
        }
        return null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}