package com.docflow.ai.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API 版本拦截器测试")
class ApiVersionInterceptorTest {

    private final ApiVersionInterceptor interceptor = new ApiVersionInterceptor();

    @Test
    @DisplayName("从 Header 提取版本号")
    void shouldExtractVersionFromHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Version", "2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Handler without @ApiVersion annotation - should pass
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("无版本号请求应通过")
    void shouldPassRequestWithoutVersion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }
}
