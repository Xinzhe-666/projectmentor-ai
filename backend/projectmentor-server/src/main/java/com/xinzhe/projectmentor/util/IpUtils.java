package com.xinzhe.projectmentor.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IpUtils {

    private static final int MAX_IP_LENGTH = 64;

    private IpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (isUsable(firstIp)) {
                return normalize(firstIp);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (isUsable(realIp)) {
            return normalize(realIp);
        }

        return normalize(request.getRemoteAddr());
    }

    private static boolean isUsable(String value) {
        return StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value.trim());
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String normalized = value.trim();
        return normalized.length() <= MAX_IP_LENGTH
                ? normalized
                : normalized.substring(0, MAX_IP_LENGTH);
    }
}
