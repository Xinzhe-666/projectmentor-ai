package com.xinzhe.projectmentor.config;

import com.xinzhe.projectmentor.admin.interceptor.AdminInterceptor;
import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.interceptor.AuthInterceptor;
import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebMvcConfigAuthExclusionTests {

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

    @Test
    void emailCodeEndpointDoesNotRequireLogin() throws Exception {
        mockMvc.perform(post("/api/auth/email-code"))
                .andExpect(status().isOk());
    }

    @Test
    void normalApiEndpointStillRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        WebMvcConfig webMvcConfig() {
            return new WebMvcConfig(new AuthInterceptor(mock(JwtUtil.class)), new AdminInterceptor(mock(AdminService.class)));
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/api/auth/email-code")
        Result<Void> emailCode() {
            return Result.success();
        }

        @PostMapping("/api/projects")
        Result<Void> projects() {
            return Result.success();
        }
    }
}
