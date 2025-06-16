package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "feature")
public class FeatureProperties {

  private boolean searchByManager = false;
  private boolean handleUser = false;
  private boolean workingWithAql = false;
  private boolean cohortExplorer = true;
  private boolean handleContent = false;
  private boolean handleUserMessages = false;
  private boolean handleOrganization = false;
  private boolean handleProject = false;
}
