package org.highmed.numportal.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;

public class TranslatedStringValidator
    implements ConstraintValidator<ValidTranslatedString, Map<String, String>> {

  @Override
  public boolean isValid(
      Map<String, String> translationMap, ConstraintValidatorContext constraintValidatorContext) {
    if (translationMap == null) {
      return false;
    }

    return !Strings.isBlank(translationMap.get("en")) && !Strings.isBlank(translationMap.get("de"));
  }
}
