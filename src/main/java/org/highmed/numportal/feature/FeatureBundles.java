package org.highmed.numportal.feature;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "feature")
public class FeatureBundles {

  private boolean cohortExplorer = false;
  private boolean core = true;
  private boolean searchByManager = false;

  public boolean isFeatureActive(String[] features) {
    for (String feature : features) {
      switch (feature) {
        case "feature.core":
          if (core) {
            return true;
          }
          break;
        case "feature.cohort-explorer":
          if (cohortExplorer) {
            return true;
          }
          break;
        case "feature.search-by-manager":
          if (searchByManager) {
            return true;
          }
          break;
        default:
          break;
      }
    }
    return false;
  }
}
