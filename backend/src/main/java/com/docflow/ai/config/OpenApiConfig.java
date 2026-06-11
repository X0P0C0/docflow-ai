package com.docflow.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI docflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocFlow AI API Documentation")
                        .description("DocFlow AI - AI-driven intelligent customer service ticket platform. "
                                + "Provides ticket management, knowledge base, AI analysis, and customer portal APIs.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DocFlow AI Team")
                                .email("team@docflow.ai"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
