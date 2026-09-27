package com.xinzhe.projectmentor.analysis.service;

import java.util.regex.Pattern;

public final class SafePipelineError {

    private static final Pattern JDBC_URL = Pattern.compile("jdbc:[^\\s,;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern URI_CREDENTIALS = Pattern.compile(
            "(?i)(amqps?://)[^@\\s]+@"
    );
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[_-]?key|authorization)\\s*[=:]\\s*[^\\s,;]+"
    );
    private static final Pattern SENSITIVE_CONTENT = Pattern.compile(
            "(?i)(prompt|source|readme|content)\\s*[=:]\\s*[^,;]+"
    );
    private static final int MAX_LENGTH = 500;

    private SafePipelineError() {
    }

    public static String from(Throwable throwable) {
        String type = throwable == null ? "Unknown" : throwable.getClass().getSimpleName();
        String message = throwable == null ? "unknown failure" : throwable.getMessage();
        return sanitize(type + ": " + (message == null ? "no message" : message));
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unspecified failure";
        }
        String sanitized = JDBC_URL.matcher(value).replaceAll("[jdbc-url-redacted]");
        sanitized = URI_CREDENTIALS.matcher(sanitized).replaceAll("$1[credentials-redacted]@");
        sanitized = SECRET.matcher(sanitized).replaceAll("$1=[redacted]");
        sanitized = SENSITIVE_CONTENT.matcher(sanitized).replaceAll("$1=[redacted]");
        sanitized = sanitized.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH);
    }
}
