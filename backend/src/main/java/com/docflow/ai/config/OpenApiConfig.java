package com.docflow.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI docflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocFlow AI 接口文档")
                        .description("DocFlow AI 工单管理与知识库系统 —— 后端 REST API 接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DocFlow AI Team")));
    }
}