package org.highmed.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ehrbase")
public class EhrBaseProperties {

  private String restApiUrl;

  private String username;

  private String password;

  private String adminUsername;

  private String adminPassword;


}
