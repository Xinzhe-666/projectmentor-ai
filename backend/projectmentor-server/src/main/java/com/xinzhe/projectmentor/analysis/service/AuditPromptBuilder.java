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
                你是一名严格的项目真实性审计专家和技术面试官。

                你只能基于下面提供的项目基础信息、规则扫描风险和证据链分析，生成客观审计结果。
                README 声明不等于代码证据；如果某项能力缺少代码、配置、SQL、部署文件或规则证据支撑，必须明确说“缺少证据”。
                简历描述不得编造项目没有的功能，不得把计划、设想或 README 包装词写成已经实现的能力。

                输出要求：
                1. 只能输出 JSON，不要输出 Markdown，不要输出解释文本。
                2. JSON 字段固定为 summary、strengths、weaknesses、suggestions、resumeBasic、resumeStandard、resumeAdvanced。
                3. 不要写虚假 QPS、高并发、微服务、生产级、高可用、海量数据等没有证据的结论。
                4. 如果证据不足，resumeAdvanced 必须明确提示不建议使用冲刺版描述。

                JSON 格式必须严格如下：
                {
                  "summary": "一句话总结项目当前状态",
                  "strengths": "基于证据能够确认的真实优点",
                  "weaknesses": "主要不足和缺少证据的地方",
                  "suggestions": "可执行修改建议",
                  "resumeBasic": "简历保守版描述，只写有证据的能力",
                  "resumeStandard": "简历标准版描述，仍然不能编造",
                  "resumeAdvanced": "简历冲刺版描述，如果当前证据不足请明确不建议使用冲刺版"
                }

                项目基础信息：
                %s

                当前文件数量：%s
                当前风险数量：%s
                HIGH 风险数量：%s
                MEDIUM 风险数量：%s
                LOW 风险数量：%s

                规则扫描风险：
                %s

                证据链：
                %s

                规则建议：
                %s
                """.formatted(
                toJson(project),
                safeCount(scanResult.getFileCount()),
                safeCount(scanResult.getTotalRiskCount()),
                safeCount(scanResult.getHighRiskCount()),
                safeCount(scanResult.getMediumRiskCount()),
                safeCount(scanResult.getLowRiskCount()),
                toJson(scanResult.getRisks()),
                toJson(scanResult.getEvidences()),
                toJson(scanResult.getSuggestions())
        );
    }

    private Integer safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Prompt JSON 序列化失败");
        }
    }
}
