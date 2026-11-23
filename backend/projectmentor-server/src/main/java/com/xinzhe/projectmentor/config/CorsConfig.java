package com.xinzhe.projectmentor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins, Environment environment) {
        this.allowedOrigins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toArray(String[]::new);

        if (this.allowedOrigins.length == 0) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must not be empty");
        }
        if (environment.matchesProfiles("prod")
                && Arrays.stream(this.allowedOrigins).anyMatch(origin -> !isProductionOrigin(origin))) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS in prod must contain exact HTTPS DNS origins only");
        }
    }

    private static boolean isProductionOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getRawUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            String domain = host.toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String authority = host + (port == -1 ? "" : ":" + port);
            return domain.contains(".") && domain.matches("[a-z0-9.-]+\\.[a-z][a-z0-9-]*")
                    && !domain.endsWith(".localhost") && !domain.endsWith(".local")
                    && port != 0 && port <= 65535 && authority.equals(uri.getRawAuthority());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
