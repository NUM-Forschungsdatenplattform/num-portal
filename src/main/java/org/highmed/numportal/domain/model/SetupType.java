package org.highmed.numportal.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

public enum SetupType {
  PREPROD,
  PROD,
  DEV,
  STAGING;

  @AllArgsConstructor
  @Getter
  public enum Preprod {
    NUM("https://num-portal.crr.pre-prod.num-codex.de/admin/health"),
    EHRBASE("https://ehrbase.crr.pre-prod.num-codex.de/ehrbase/management/health/readiness"),
    FHIR_BRIDGE("https://fhir-bridge.crr.pre-prod.num-codex.de/fhir-bridge/actuator/health/readiness"),
    FE("https://pre-prod.num-codex.de/home"),
    KEYCLOAK("https://keycloak.pre-prod.num-codex.de");
    private String URL;
  }
  
  @AllArgsConstructor
  @Getter
  public enum Prod {
    NUM("https://num-portal.crr.num-codex.de/admin/health"),
    EHRBASE("https://ehrbase.crr.num-codex.de/ehrbase/management/health/readiness"),
    FHIR_BRIDGE("https://fhir-bridge.crr.num-codex.de/fhir-bridge/actuator/health/readiness"),
    FE("https://num-codex.de/"),
    KEYCLOAK("https://keycloak.crr.num-codex.de");
    private String URL;
  }

  @AllArgsConstructor
  @Getter
  public enum Dev {
    NUM("https://num-portal.crr.dev.num-codex.de/admin/health"),
    EHRBASE("https://ehrbase.crr.dev.num-codex.de/ehrbase/management/health/readiness"),
    FHIR_BRIDGE("https://fhir-bridge.crr.dev.num-codex.de/fhir-bridge/actuator/health/readiness"),
    FE("https://dev.num-codex.de/"),
    KEYCLOAK("https://keycloak.dev.num-codex.de");
    private String URL;
  }

  @AllArgsConstructor
  @Getter
  public enum Staging {
    NUM("https://num-portal.crr.staging.num-codex.de/admin/health"),
    EHRBASE("https://ehrbase.crr.staging.num-codex.de/ehrbase/management/health/readiness"),
    FHIR_BRIDGE("https://fhir-bridge.crr.staging.num-codex.de/fhir-bridge/actuator/health/readiness"),
    FE("https://staging.num-codex.de/"),
    KEYCLOAK("https://keycloak.staging.num-codex.de");
    private String URL;
  }
}
