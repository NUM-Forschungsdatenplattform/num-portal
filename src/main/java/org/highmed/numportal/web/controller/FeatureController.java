package org.highmed.numportal.web.controller;

import org.highmed.numportal.feature.FeatureBundles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/feature", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class FeatureController {
  FeatureBundles featureBundles;

  @GetMapping
  @Operation(description = "Get feature bundles")
  public ResponseEntity<FeatureBundles> getFeatureBundles() {
    return ResponseEntity.ok(featureBundles);
  }
}
