package com.xinzhe.projectmentor.retrieval.chunk;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SensitiveContentRedactor {
    private static final String MASK = "[REDACTED]";
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(?i)(api[_-]?key|access[_-]?token|secret|password|passwd|pwd)\\s*[:=]\\s*[\\\"']?([^\\s\\\"',;}{]{6,})"),
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)([a-z0-9._~+/-]{8,})"),
            Pattern.compile("(?i)(-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)[\\s\\S]*?(-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)"),
            Pattern.compile("\\b(?:sk|rk|pk)-[A-Za-z0-9_-]{16,}\\b")
    );

    public RedactionResult redact(String input) {
        String safe = input == null ? "" : input;
        int matches = 0;
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(safe);
            StringBuffer output = new StringBuffer();
            while (matcher.find()) {
                matches++;
                String replacement;
                if (matcher.groupCount() >= 2 && matcher.group(1) != null) {
                    replacement = Matcher.quoteReplacement(matcher.group(1) + MASK);
                } else {
                    replacement = MASK;
                }
                matcher.appendReplacement(output, replacement);
            }
            matcher.appendTail(output);
            safe = output.toString();
        }
        return new RedactionResult(safe, matches);
    }

    public record RedactionResult(String content, int redactionCount) {
        public boolean redacted() {
            return redactionCount > 0;
        }
    }
}
