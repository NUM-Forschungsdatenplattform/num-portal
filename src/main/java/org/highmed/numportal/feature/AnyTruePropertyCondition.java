package org.highmed.numportal.feature;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.Map;

public class AnyTruePropertyCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    if (!metadata.isAnnotated(ConditionalOnAnyProperty.class.getName())) {
      return true;
    }

    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAnyProperty.class.getName());
    String[] properties = (String[]) attributes.get("value");

    Environment env = context.getEnvironment();

    return Arrays.stream(properties).map(p -> env.getProperty(p, Boolean.class, false)).anyMatch(Boolean::booleanValue);
  }
}
