package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditPromptBuilder {

    private final ObjectMapper objectMapper;

    public String build(Project project, RuleScanResultVO scanResult) {
        return """
                你是一名严格的大厂 Java 后端面试官和项目真实性审计专家。

                请基于下面的项目基础信息、规则扫描结果、风险点和证据链，生成一份客观的项目审计结果。

                重要要求：
                1. 不要鼓励式夸奖。
                2. 不要编造项目没有实现的功能。
                3. 如果缺少证据，要明确说缺少证据。
                4. 简历描述必须真实可信。
                5. 不要写虚假 QPS、高并发、微服务、生产级、高可用。
                6. 必须基于规则扫描结果和证据链进行分析。
                7. 只输出 JSON，不要输出 Markdown，不要输出解释文本。

                JSON 格式必须如下：
                {
                  "summary": "一句话总结项目当前状态",
                  "strengths": "项目真实优点",
                  "weaknesses": "项目主要不足",
                  "suggestions": "可执行修改建议",
                  "resumeBasic": "简历保守版描述",
                  "resumeStandard": "简历标准版描述",
                  "resumeAdvanced": "简历冲刺版描述，如果当前证据不足请明确不建议使用冲刺版"
                }

                项目基础信息：
                %s

                规则扫描结果：
                %s
                """.formatted(toJson(project), toJson(scanResult));
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Prompt JSON 序列化失败");
        }
    }
}