package com.xinzhe.projectmentor.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiJsonUtil {

    private final ObjectMapper objectMapper;

    public String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }

        String trimmed = content.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring("```json".length()).trim();
        }

        if (trimmed.startsWith("```JSON")) {
            trimmed = trimmed.substring("```JSON".length()).trim();
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

    public JsonNode safeReadTree(String content) {
        try {
            return objectMapper.readTree(extractJson(content));
        } catch (Exception e) {
            return null;
        }
    }

    public String getText(JsonNode root, String fieldName) {
        if (root == null || fieldName == null || fieldName.isBlank()) {
            return "";
        }

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
