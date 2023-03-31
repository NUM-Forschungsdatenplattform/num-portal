package de.vitagroup.num.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fttp")
public class FttpProperties {

  private String url;

  private String certificatePath;

  private String certificateKey;

  private boolean useBasicAuth;

  private String username;

  private String password;
}
