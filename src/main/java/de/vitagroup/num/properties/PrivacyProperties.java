package de.vitagroup.num.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "privacy")
public class PrivacyProperties {

  public static final String RESULTS_WITHHELD_FOR_PRIVACY_REASONS =
      "Number of matches below threshold, results withheld for privacy reasons.";

  private int minHits = 50;
  private String pseudonymitySecret;
}
