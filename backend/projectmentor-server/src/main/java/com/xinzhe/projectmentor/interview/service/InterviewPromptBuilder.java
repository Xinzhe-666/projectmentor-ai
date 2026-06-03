package com.xinzhe.projectmentor.interview.service;

import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.interview.entity.InterviewMessage;
import com.xinzhe.projectmentor.project.entity.Project;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewPromptBuilder {

    private static final int MAX_FILE_COUNT = 12;
    private static final int MAX_FILE_SNIPPET_CHARS = 400;
    private static final int MAX_HISTORY_COUNT = 8;

    public String buildSystemPrompt() {
        return """
                你是 ProjectMentor AI 的项目面试官。你必须基于项目基础信息、项目文件摘要、历史问答和用户回答进行追问。
                不要编造项目没有的功能；README 声明不等于代码证据。
                每个问题都必须绑定证据强度：STRONG / MEDIUM / WEAK / NONE。
                如果只有 README 描述，没有代码证据，必须明确说“README 中有描述，但当前代码证据不足……”。
                如果只有文档、配置或部署证据，不要追问核心训练、算法或复杂实现细节，除非文件摘要里有对应代码证据。
                如果证据不足，使用“当前证据不足，请说明你是否实际实现了……”这类克制表达。
                sourceFile 只能填写项目文件摘要中真实存在的 path；没有真实关联文件时留空。
                每次只问一个问题。
                如果用户回答空泛，优先追问可解释流程、证据来源和边界，不要无证据地追问过细类名。
                如果用户提到 JWT、Redis、规则扫描、证据链，继续追问实现细节。
                只输出 JSON，不要输出 Markdown，不要解释。
                """;
    }

    public String buildFirstQuestionPrompt(Project project, String mode, List<ProjectFile> files) {
        return """
                请基于下面的项目上下文，生成模拟面试的第一个问题。
                要求：
                1. 每次只问一个问题。
                2. 不要编造项目没有的功能。
                3. 默认一轮面试最多 8 个核心问题，本题是第 1 题，优先放在“项目真实性”。
                4. 问题分类只能从 PROJECT_REALITY、TECH_IMPLEMENTATION、EVIDENCE_EXPLANATION、RESUME_RISK、PRESSURE_FOLLOW_UP 中选择。
                5. 如果没有代码证据，不要确定性假设用户实现了某功能。
                6. sourceFile 只能填写项目文件摘要里真实存在的 path。

                输出 JSON：
                {
                  "question": "一个自然、具体、可回答的面试问题",
                  "category": "PROJECT_REALITY",
                  "evidenceStrength": "STRONG/MEDIUM/WEAK/NONE",
                  "sourceFile": "真实存在的关联文件路径，没有则留空",
                  "reason": "为什么这个问题由当前证据触发"
                }

                面试模式：%s

                项目基础信息：
                - name: %s
                - type: %s
                - techStack: %s
                - description: %s

                项目文件摘要：
                %s
                """.formatted(
                safe(mode),
                safe(project.getName()),
                safe(project.getProjectType()),
                safe(project.getTechStack()),
                safe(project.getDescription()),
                buildFileSummary(files)
        );
    }

    public String buildEvaluationPrompt(Project project,
                                        String mode,
                                        String answer,
                                        int ruleScore,
                                        String ruleFeedback,
                                        String ruleFollowUp,
                                        List<ProjectFile> files,
                                        List<InterviewMessage> history) {
        return """
                请基于项目上下文和用户回答，给出评分、反馈和下一轮追问。
                要求：
                1. score 必须是 0-100 的整数。
                2. feedback 要指出回答中具体、不足和需要补证据的地方。
                3. followUpQuestion 每次只问一个问题。
                4. 不要编造项目没有的功能。
                5. 如果用户回答空泛，追问可解释流程、证据来源和边界。
                6. 如果用户提到 JWT、Redis、规则扫描、证据链，只有存在代码/配置证据时才追问实现细节；证据不足时使用克制表达。
                7. 当前面试最多 8 个核心问题，不要制造无限追问。
                8. followUpCategory 只能从 PROJECT_REALITY、TECH_IMPLEMENTATION、EVIDENCE_EXPLANATION、RESUME_RISK、PRESSURE_FOLLOW_UP 中选择。
                9. sourceFile 只能填写项目文件摘要里真实存在的 path。

                输出 JSON：
                {
                  "score": 0-100,
                  "feedback": "对用户回答的反馈",
                  "followUpQuestion": "下一轮追问",
                  "followUpCategory": "TECH_IMPLEMENTATION",
                  "evidenceStrength": "STRONG/MEDIUM/WEAK/NONE",
                  "sourceFile": "真实存在的关联文件路径，没有则留空",
                  "reason": "为什么这个追问由当前证据或回答触发"
                }

                面试模式：%s

                项目基础信息：
                - name: %s
                - type: %s
                - techStack: %s
                - description: %s

                项目文件摘要：
                %s

                历史消息：
                %s

                规则版评分参考：
                - score: %s
                - feedback: %s
                - followUpQuestion: %s

                用户本轮回答：
                %s
                """.formatted(
                safe(mode),
                safe(project.getName()),
                safe(project.getProjectType()),
                safe(project.getTechStack()),
                safe(project.getDescription()),
                buildFileSummary(files),
                buildHistorySummary(history),
                ruleScore,
                safe(ruleFeedback),
                safe(ruleFollowUp),
                safe(answer)
        );
    }

    private String buildFileSummary(List<ProjectFile> files) {
        if (files == null || files.isEmpty()) {
            return "未提供项目文件摘要。";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (ProjectFile file : files) {
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

    private String buildHistorySummary(List<InterviewMessage> history) {
        if (history == null || history.isEmpty()) {
            return "暂无历史消息。";
        }

        int start = Math.max(0, history.size() - MAX_HISTORY_COUNT);
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            InterviewMessage message = history.get(i);
            builder.append("- ")
                    .append(safe(message.getRole()))
                    .append(": ")
                    .append(trim(safe(message.getContent()), 500))
                    .append("\n");
        }

        return builder.toString();
    }

    private String trim(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }

        return text.substring(0, maxChars) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
