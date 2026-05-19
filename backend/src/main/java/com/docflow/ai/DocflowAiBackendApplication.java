package com.docflow.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocflowAiBackendApplication {

    public static void main(String[] args) {
        // 后端入口保持极简：真正的装配都在 Spring 配置和各模块 service 中完成。
        SpringApplication.run(DocflowAiBackendApplication.class, args);
    }
}
