package com.docflow.ai.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiWorkspacePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldUseDefaultClaimStaleAfterDuration() {
        contextRunner.run(context -> {
            AiWorkspaceProperties properties = context.getBean(AiWorkspaceProperties.class);
            assertThat(properties.getClaimStaleAfter()).isEqualTo(Duration.ofHours(2));
        });
    }

    @Test
    void shouldBindCustomClaimStaleAfterDuration() {
        contextRunner
                .withPropertyValues("app.ai.claim-stale-after=45m")
                .run(context -> {
                    AiWorkspaceProperties properties = context.getBean(AiWorkspaceProperties.class);
                    assertThat(properties.getClaimStaleAfter()).isEqualTo(Duration.ofMinutes(45));
                });
    }

    @Configuration
    @EnableConfigurationProperties(AiWorkspaceProperties.class)
    static class TestConfiguration {
    }
}
