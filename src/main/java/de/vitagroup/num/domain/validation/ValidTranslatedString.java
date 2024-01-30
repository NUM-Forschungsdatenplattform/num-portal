package de.vitagroup.num.domain.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = TranslatedStringValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface ValidTranslatedString {

    String message() default "Translated string must be a valid one";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
