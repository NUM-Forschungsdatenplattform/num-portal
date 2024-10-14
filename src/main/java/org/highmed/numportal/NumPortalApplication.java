package org.highmed.numportal;

import org.highmed.numportal.properties.FeatureProperties;
import org.highmed.numportal.service.atna.AtnaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties({AtnaProperties.class, FeatureProperties.class})
public class NumPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(NumPortalApplication.class, args);
  }
}
