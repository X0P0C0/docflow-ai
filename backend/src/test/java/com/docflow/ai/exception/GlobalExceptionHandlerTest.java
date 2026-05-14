package com.docflow.ai.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void businessExceptionShouldReturnConflictStatusAndUnifiedBody() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.error").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("当前工单已经指派给该处理人"))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    void validationExceptionShouldReturnBadRequestStatusAndFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void unhandledExceptionShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        public void business() {
            throw new BusinessException(com.docflow.ai.common.enums.ResultCode.RESOURCE_CONFLICT, "当前工单已经指派给该处理人");
        }

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/error")
        public void error() {
            throw new IllegalStateException("boom");
        }
    }

    static class TestRequest {

        @NotBlank(message = "name 不能为空")
        private final String name;

        @JsonCreator
        TestRequest(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
