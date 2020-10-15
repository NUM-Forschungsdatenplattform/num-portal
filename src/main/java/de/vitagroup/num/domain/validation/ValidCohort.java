package de.vitagroup.num.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = CohortValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface ValidCohort {

    String message() default "Cohort must be a valid one";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
