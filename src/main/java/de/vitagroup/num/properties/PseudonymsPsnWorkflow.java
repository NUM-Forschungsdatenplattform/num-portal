package de.vitagroup.num.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "requestpsnworkflow.params")
public class PseudonymsPsnWorkflow {

    private String study;

    private String source;

    private String target;

    private String apiKey;

    private String event;
}
