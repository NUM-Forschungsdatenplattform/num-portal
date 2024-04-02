package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "requestpsnworkflow.params")
public class PseudonymsPsnWorkflowProperties {

    private String study;

    private String source;

    private String target;

    private String apiKey;

    private String event;
}
