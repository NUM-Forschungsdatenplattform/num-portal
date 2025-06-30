package org.highmed.numportal.feature;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "feature")
public class FeatureBundles {

  private boolean searchByManager = false;
  private boolean numPortal = true; //ToDo better naming
  private boolean cohortExplorer = false;
}
