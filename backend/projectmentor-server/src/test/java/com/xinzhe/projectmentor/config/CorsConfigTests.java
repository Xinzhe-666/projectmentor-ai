package com.xinzhe.projectmentor.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTests {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebConfig.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://projectmentorai.com",
            "https://www.projectmentorai.com",
            "http://localhost:5173",
            "http://127.0.0.1:3000"
    })
    void loginPreflightAllowsConfiguredOrigins(String origin) throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"));
    }

    @Test
    void loginPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://malicious.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/login", "/api/auth/register", "/api/auth/email-code"})
    void authPostPreflightAllowsProductionOrigin(String path) throws Exception {
        mockMvc.perform(options(path)
                        .header(HttpHeaders.ORIGIN, "https://projectmentorai.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://projectmentorai.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/health", "/api/share/reports/example-token"})
    void publicGetPreflightAllowsProductionOrigin(String path) throws Exception {
        mockMvc.perform(options(path)
                        .header(HttpHeaders.ORIGIN, "https://projectmentorai.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://projectmentorai.com"));
    }

    @Test
    void bearerApiPreflightAllowsAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/projects")
                        .header(HttpHeaders.ORIGIN, "https://projectmentorai.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://projectmentorai.com"))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        HttpHeaders.AUTHORIZATION));
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        CorsConfig corsConfig() {
            return new CorsConfig(new String[]{
                    "https://projectmentorai.com",
                    "https://www.projectmentorai.com",
                    "http://projectmentorai.com",
                    "http://www.projectmentorai.com",
                    "http://8.218.121.30",
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://localhost:3000",
                    "http://127.0.0.1:3000"
            });
        }

        @Bean
        CorsTestController corsTestController() {
            return new CorsTestController();
        }
    }

    @RestController
    static class CorsTestController {

        @PostMapping("/api/auth/login")
        void login() {
        }

        @PostMapping("/api/auth/register")
        void register() {
        }

        @PostMapping("/api/auth/email-code")
        void emailCode() {
        }

        @GetMapping("/api/health")
        void health() {
        }

        @GetMapping("/api/share/reports/{token}")
        void shareReport() {
        }

        @GetMapping("/api/projects")
        void projects() {
        }
    }
}
