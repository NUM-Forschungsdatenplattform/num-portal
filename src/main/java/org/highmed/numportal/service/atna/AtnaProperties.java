package org.highmed.numportal.service.atna;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "atna")
public class AtnaProperties {

  private String host;

  private int port;

  private boolean enabled;
}