package de.vitagroup.num.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "num")
public class NumProperties {

  private String locale;
  private String url;
  private String systemStatusUrl;
  private Map<String, String> userManualUrl;

}
