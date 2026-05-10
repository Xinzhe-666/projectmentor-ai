package com.xinzhe.projectmentor.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.dto.AiAuditResult;
import com.xinzhe.projectmentor.ai.dto.ChatCompletionRequest;
import com.xinzhe.projectmentor.ai.dto.ChatCompletionResponse;
import com.xinzhe.projectmentor.ai.dto.ChatMessage;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient implements LlmClient {

    private final AiProperties aiProperties;

    private final ObjectMapper objectMapper;

    private final RestTemplateBuilder restTemplateBuilder;

    @Override
    public AiAuditResult generateAuditReport(String prompt) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI_API_KEY 未配置，无法调用 AI 服务");
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .setReadTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(aiProperties.getModel())
                    .temperature(0.2)
                    .maxTokens(1600)
                    .messages(List.of(
                            new ChatMessage("system", "你是一名严格的大厂 Java 后端面试官和项目真实性审计专家。你必须基于用户提供的规则扫描结果和证据链分析，不允许编造项目没有的功能。请尽量输出 JSON。"),
                            new ChatMessage("user", prompt)
                    ))
                    .build();

            String url = normalizeBaseUrl(aiProperties.getBaseUrl()) + "/v1/chat/completions";

            ChatCompletionResponse response = restTemplate.postForObject(
                    url,
                    request,
                    ChatCompletionResponse.class
            );

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 返回为空");
            }

            String content = response.getChoices().get(0).getMessage().getContent();

            return parseAuditResult(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI service call failed", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务调用失败：" + e.getMessage());
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com";
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }

    private AiAuditResult parseAuditResult(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);

            return AiAuditResult.builder()
                    .summary(getText(root, "summary"))
                    .strengths(getText(root, "strengths"))
                    .weaknesses(getText(root, "weaknesses"))
                    .suggestions(getText(root, "suggestions"))
                    .resumeBasic(getText(root, "resumeBasic"))
                    .resumeStandard(getText(root, "resumeStandard"))
                    .resumeAdvanced(getText(root, "resumeAdvanced"))
                    .build();
        } catch (Exception e) {
            log.warn("AI response is not valid JSON, fallback to raw content. content={}", content);

            return AiAuditResult.builder()
                    .summary(content)
                    .strengths("AI 返回内容未能解析为结构化 JSON，请查看 summary 原文。")
                    .weaknesses("AI 结构化解析失败，后续可优化 Prompt 或增加 JSON 修复逻辑。")
                    .suggestions("建议检查 AI 返回格式，并要求模型严格输出 JSON。")
                    .resumeBasic("")
                    .resumeStandard("")
                    .resumeAdvanced("")
                    .build();
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }

        String trimmed = content.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring("```json".length()).trim();
        }

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring("```".length()).trim();
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        int firstBrace = trimmed.indexOf("{");
        int lastBrace = trimmed.lastIndexOf("}");

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    private String getText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);

        if (node == null || node.isNull()) {
            return "";
        }

        if (node.isTextual()) {
            return node.asText();
        }

        return node.toString();
    }
}