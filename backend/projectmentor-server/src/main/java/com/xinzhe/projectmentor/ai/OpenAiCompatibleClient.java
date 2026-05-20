package com.xinzhe.projectmentor.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinzhe.projectmentor.ai.dto.AiAuditResult;
import com.xinzhe.projectmentor.ai.dto.ChatCompletionRequest;
import com.xinzhe.projectmentor.ai.dto.ChatCompletionResponse;
import com.xinzhe.projectmentor.ai.dto.ChatMessage;
import com.xinzhe.projectmentor.ai.entity.AiCallLog;
import com.xinzhe.projectmentor.ai.mapper.AiCallLogMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
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

    private static final String REPORT_SYSTEM_PROMPT = """
            你是一名严格的项目真实性审计专家。你只能基于用户提供的规则扫描结果、证据链和项目文件信息分析。
            README 声明不等于代码证据；如果缺少证据，必须明确说明缺少证据。
            不允许编造项目没有的功能，不允许输出虚假 QPS、高并发、微服务、生产级、高可用等结论。
            只输出 JSON，不要输出 Markdown 或解释文本。
            """;

    private final AiProperties aiProperties;

    private final AiJsonUtil aiJsonUtil;

    private final RestTemplateBuilder restTemplateBuilder;

    private final AiCallLogMapper aiCallLogMapper;

    @Override
    public AiAuditResult generateAuditReport(String prompt) {
        String content = chat("REPORT", REPORT_SYSTEM_PROMPT, prompt);
        return parseAuditResult(content);
    }

    @Override
    public String chat(String module, String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();
        String normalizedModule = normalizeModule(module);
        String effectiveSystemPrompt = safe(systemPrompt);
        String effectiveUserPrompt = truncateUserPrompt(safe(userPrompt));
        int promptChars = effectiveSystemPrompt.length() + effectiveUserPrompt.length();

        boolean success = false;
        String content = null;
        String errorMessage = null;

        try {
            validateAiAvailable();

            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .setReadTimeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(resolveModel())
                    .temperature(0.2)
                    .maxTokens(resolveMaxResponseTokens())
                    .messages(List.of(
                            new ChatMessage("system", effectiveSystemPrompt),
                            new ChatMessage("user", effectiveUserPrompt)
                    ))
                    .build();

            ChatCompletionResponse response = restTemplate.postForObject(
                    normalizeBaseUrl(aiProperties.getBaseUrl()) + "/v1/chat/completions",
                    request,
                    ChatCompletionResponse.class
            );

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 返回为空");
            }

            ChatCompletionResponse.Message message = response.getChoices().get(0).getMessage();
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 返回内容为空");
            }

            content = message.getContent();
            success = true;
            return content;
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            throw e;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.warn("AI service call failed: module={}, model={}, message={}",
                    normalizedModule,
                    resolveModel(),
                    e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务调用失败：" + safe(e.getMessage()));
        } finally {
            long latencyMs = System.currentTimeMillis() - startTime;
            recordCall(
                    normalizedModule,
                    success,
                    promptChars,
                    content == null ? 0 : content.length(),
                    latencyMs,
                    errorMessage
            );
        }
    }

    private void validateAiAvailable() {
        if (!Boolean.TRUE.equals(aiProperties.getEnabled())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务未启用");
        }

        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI_API_KEY 未配置");
        }
    }

    private String truncateUserPrompt(String userPrompt) {
        int maxPromptChars = resolveMaxPromptChars();
        if (userPrompt.length() <= maxPromptChars) {
            return userPrompt;
        }

        String note = "\n\n【系统提示：原始用户提示超过限制，部分内容已截断。】";
        int keepChars = Math.max(0, maxPromptChars - note.length());
        return userPrompt.substring(0, Math.min(userPrompt.length(), keepChars)) + note;
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
            JsonNode root = aiJsonUtil.safeReadTree(content);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("AI response is not a JSON object");
            }

            return AiAuditResult.builder()
                    .summary(aiJsonUtil.getText(root, "summary"))
                    .strengths(aiJsonUtil.getText(root, "strengths"))
                    .weaknesses(aiJsonUtil.getText(root, "weaknesses"))
                    .suggestions(aiJsonUtil.getText(root, "suggestions"))
                    .resumeBasic(aiJsonUtil.getText(root, "resumeBasic"))
                    .resumeStandard(aiJsonUtil.getText(root, "resumeStandard"))
                    .resumeAdvanced(aiJsonUtil.getText(root, "resumeAdvanced"))
                    .build();
        } catch (Exception e) {
            log.warn("AI audit response is not valid JSON, fallback to raw summary. responseChars={}",
                    content == null ? 0 : content.length());

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

    private void recordCall(String module,
                            boolean success,
                            Integer promptChars,
                            Integer responseChars,
                            Long latencyMs,
                            String errorMessage) {
        try {
            AiCallLog callLog = new AiCallLog();
            callLog.setUserId(resolveUserId());
            callLog.setModule(module);
            callLog.setModel(resolveModel());
            callLog.setSuccess(success ? 1 : 0);
            callLog.setPromptChars(promptChars);
            callLog.setResponseChars(responseChars);
            callLog.setLatencyMs(latencyMs);
            callLog.setErrorMessage(truncateError(errorMessage));

            aiCallLogMapper.insert(callLog);
        } catch (Exception e) {
            log.warn("Failed to record AI call log: module={}, message={}", module, e.getMessage());
        }
    }

    private Long resolveUserId() {
        try {
            return UserContext.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return "UNKNOWN";
        }

        return module.trim().toUpperCase();
    }

    private String resolveModel() {
        if (aiProperties.getModel() == null || aiProperties.getModel().isBlank()) {
            return "deepseek-chat";
        }

        return aiProperties.getModel();
    }

    private int resolveTimeoutSeconds() {
        Integer timeoutSeconds = aiProperties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 60 : timeoutSeconds;
    }

    private int resolveMaxPromptChars() {
        Integer maxPromptChars = aiProperties.getMaxPromptChars();
        return maxPromptChars == null || maxPromptChars <= 0 ? 12000 : maxPromptChars;
    }

    private int resolveMaxResponseTokens() {
        Integer maxResponseTokens = aiProperties.getMaxResponseTokens();
        return maxResponseTokens == null || maxResponseTokens <= 0 ? 1600 : maxResponseTokens;
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
