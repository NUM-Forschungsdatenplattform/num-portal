package de.vitagroup.num.service.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "zars")
public class ZarsProperties {

  private boolean enabled;
  private String email;
}
