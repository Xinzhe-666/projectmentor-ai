package com.xinzhe.projectmentor.ai.controller;

import com.xinzhe.projectmentor.ai.AiProperties;
import com.xinzhe.projectmentor.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProperties aiProperties;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        boolean enabled = Boolean.TRUE.equals(aiProperties.getEnabled());
        boolean configured = aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", enabled);
        data.put("configured", configured);
        data.put("model", safeDefault(aiProperties.getModel(), "deepseek-chat"));
        data.put("baseUrl", safeDefault(aiProperties.getBaseUrl(), "https://api.deepseek.com"));
        data.put("message", buildMessage(enabled, configured));

        return Result.success(data);
    }

    private String buildMessage(boolean enabled, boolean configured) {
        if (!enabled) {
            return "AI 服务未启用，系统将使用规则版降级能力";
        }

        if (!configured) {
            return "AI_API_KEY 未配置，系统将使用规则版降级能力";
        }

        return "AI 增强已配置";
    }

    private String safeDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
