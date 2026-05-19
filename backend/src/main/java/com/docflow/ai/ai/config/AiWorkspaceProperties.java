package com.docflow.ai.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiWorkspaceProperties {

    private Duration claimStaleAfter = Duration.ofHours(2);
}
