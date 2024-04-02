package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "consent")
public class ConsentProperties {

  private String allowUsageOutsideEuOid;
}
