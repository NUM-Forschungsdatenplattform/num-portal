package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "feature")
public class FeatureProperties {
}
