package com.xinzhe.projectmentor.config;

import com.xinzhe.projectmentor.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityDefaultsTests {
    private static final String JWT = "explicit-test-only-jwt-secret-at-least-32-chars";
    private static final String ORIGINS = "https://projectmentorai.com,https://www.projectmentorai.com";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtUtil.class, CorsConfig.class);

    @ParameterizedTest
    @ValueSource(strings = {"", "too-short", "replace-with-a-high-entropy-secret-of-at-least-32-characters",
            "projectmentor-ai-local-dev-secret-must-be-at-least-32-chars", "this test secret contains whitespace"})
    void refusesMissingOrInvalidJwt(String value) {
        runner.withPropertyValues("JWT_SECRET=" + value).run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "JWT_SECRET must be an explicit, non-example secret of at least 32 characters without whitespace"));
    }

    @Test
    void localDefaultsAreLoopbackOnlyAndMetricsAreNotExposed() {
        runner.withPropertyValues("JWT_SECRET=" + JWT).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("app.cors.allowed-origins")).isEqualTo(
                    "http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000");
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
                    .isEqualTo("health");
        });
    }

    @Test
    void productionStartsWithExactHttpsOriginsAndExplicitJwt() {
        production().withPropertyValues("CORS_ALLOWED_ORIGINS=" + ORIGINS).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
                    .isEqualTo("health");
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.exclude"))
                    .isEqualTo("info,metrics,prometheus");
        });
    }

    @Test
    void productionRefusesEmptyOrigins() {
        production().withPropertyValues("CORS_ALLOWED_ORIGINS=").run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseMessage("CORS_ALLOWED_ORIGINS must not be empty"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example.org", "http://example.org", "https://localhost:5173",
            "https://preview.localhost", "https://127.0.0.1", "https://[::1]", "https://example.org/path",
            "https://example.org?query", "https://user@example.org", "https://example.org:", "https://example.org:99999",
            "https://projectmentorai.com,http://example.org"})
    void productionRefusesUnsafeOrigin(String origin) {
        production().withPropertyValues("CORS_ALLOWED_ORIGINS=" + origin).run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "CORS_ALLOWED_ORIGINS in prod must contain exact HTTPS DNS origins only"));
    }

    private ApplicationContextRunner production() {
        return runner.withPropertyValues("spring.profiles.active=prod", "JWT_SECRET=" + JWT);
    }
}
