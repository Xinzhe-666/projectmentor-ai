package com.xinzhe.projectmentor.claim;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ClaimTextSanitizer {

    private static final Pattern ASSIGNMENT_SECRET_PATTERN = Pattern.compile(
            "(?i)(mysql_root_password|ai_api_key|jwt_secret|api[_-]?key|apikey|password|secret|token|jwt|key)"
                    + "(\\s*[=:]\\s*)([^\\s,;]+)"
    );

    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
            "(?i)([\"']?(?:mysql_root_password|ai_api_key|jwt_secret|api[_-]?key|apikey|password|secret|token|jwt|key)[\"']?"
                    + "\\s*:\\s*[\"'])([^\"']+)([\"'])"
    );

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)[a-z0-9._~+/-]{8,}");

    public String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String sanitized = replaceAssignmentSecrets(text);
        sanitized = JSON_SECRET_PATTERN.matcher(sanitized).replaceAll("$1******$3");
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("$1******");
        return sanitized;
    }

    public String sanitizeAndLimit(String text, int maxLength) {
        String sanitized = sanitize(text)
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.length() <= maxLength) {
            return sanitized;
        }

        if (maxLength <= 3) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized.substring(0, maxLength - 3) + "...";
    }

    private String replaceAssignmentSecrets(String text) {
        Matcher matcher = ASSIGNMENT_SECRET_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + "******")
            );
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
