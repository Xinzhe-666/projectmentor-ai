package com.xinzhe.projectmentor.hallucination.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.hallucination.vo.HallucinationCheckResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HallucinationPromptBuilder {

    private static final int MAX_FILE_COUNT = 12;
    private static final int MAX_FILE_SNIPPET_CHARS = 500;

    private final ObjectMapper objectMapper;

    public String buildSystemPrompt() {
        return """
                你是 ProjectMentor AI 的幻觉检测专家。你必须基于用户粘贴的 AI 回答、项目文件摘要和规则检测结果进行判断。
                不允许编造项目没有的功能；README 声明不等于代码证据。
                如果缺少证据，请明确说明缺少证据。
                规则检测已经发现的 HIGH 风险问题不能被洗白、删除或降级。
                只输出 JSON，不要输出 Markdown，不要解释。
                """;
    }

    public String buildUserPrompt(String aiAnswer,
                                  List<ProjectFile> projectFiles,
                                  HallucinationCheckResultVO ruleResult) {
        return """
                请对下面这段 AI 回答做幻觉风险增强检测，并输出更自然、可直接展示给用户的结果。

                输出 JSON 格式：
                {
                  "credibilityScore": 0-100,
                  "objectivityScore": 0-100,
                  "riskLevel": "LOW/MEDIUM/HIGH",
                  "issues": [
                    {
                      "riskLevel": "HIGH",
                      "issueType": "OVER_ENCOURAGEMENT",
                      "matchedText": "...",
                      "message": "...",
                      "evidence": "...",
                      "suggestion": "..."
                    }
                  ],
                  "unsafeResumeStatements": ["..."],
                  "saferRewrite": "..."
                }

                用户粘贴的 AI 回答：
                %s

                项目文件摘要：
                %s

                规则检测结果：
                %s
                """.formatted(
                safe(aiAnswer),
                buildFileSummary(projectFiles),
                toJson(ruleResult)
        );
    }

    private String buildFileSummary(List<ProjectFile> projectFiles) {
        if (projectFiles == null || projectFiles.isEmpty()) {
            return "未提供项目文件摘要。";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (ProjectFile file : projectFiles) {
            if (count >= MAX_FILE_COUNT) {
                builder.append("其余文件已省略，仅用于控制 Prompt 长度。\n");
                break;
            }

            builder.append("- path: ").append(safe(file.getFilePath()))
                    .append(", type: ").append(safe(file.getFileType()))
                    .append(", contentLength: ").append(file.getContent() == null ? 0 : file.getContent().length())
                    .append("\n");

            String content = safe(file.getContent());
            if (!content.isBlank()) {
                builder.append("  snippet: ")
                        .append(content, 0, Math.min(content.length(), MAX_FILE_SNIPPET_CHARS))
                        .append("\n");
            }
            count++;
        }

        return builder.toString();
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
