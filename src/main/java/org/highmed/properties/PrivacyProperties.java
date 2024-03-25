package org.highmed.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "privacy")
public class PrivacyProperties {

  private int minHits = 50;
  private String pseudonymitySecret;
  private int pseudonomityChunksSize = 20;
}
