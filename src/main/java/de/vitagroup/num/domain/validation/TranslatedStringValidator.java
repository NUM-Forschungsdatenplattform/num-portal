package de.vitagroup.num.domain.validation;

import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;

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
