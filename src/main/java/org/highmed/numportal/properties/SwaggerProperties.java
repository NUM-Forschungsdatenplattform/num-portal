package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "swagger.oauth2")
public class SwaggerProperties {

  private String tokenUri;
  private String authUri;
  private String clientName;
  private String clientSecret;
}
