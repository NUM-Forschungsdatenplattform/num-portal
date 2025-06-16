package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "feature")
public class FeatureProperties {

  private boolean searchByManager = false;
  private boolean handleUser = true;
  private boolean workingWithAql = true;
  private boolean cohortExplorer = true;
  private boolean handleContent = true;
  private boolean handleUserMessages = true;
  private boolean handleOrganization = true;
  private boolean handleProject = true;
}
