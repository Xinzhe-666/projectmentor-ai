package com.xinzhe.projectmentor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI projectMentorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProjectMentor AI API")
                        .description("AI 项目真实性审计与面试深挖平台接口文档")
                        .version("0.0.1"));
    }
}