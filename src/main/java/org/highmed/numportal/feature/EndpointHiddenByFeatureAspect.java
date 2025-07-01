package org.highmed.numportal.feature;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EndpointHiddenByFeatureAspect {

  private final FeatureBundles featureBundles;

  @Autowired
  public EndpointHiddenByFeatureAspect(FeatureBundles featureBundles) {
    this.featureBundles = featureBundles;
  }

  @Around("@annotation(conditional)")
  public Object aroundFeatureToggledMethod(ProceedingJoinPoint pjp, ConditionalOnAnyProperty conditional) throws Throwable {
    if (!featureBundles.isFeatureActive(conditional.value())) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    return pjp.proceed();
  }


}
