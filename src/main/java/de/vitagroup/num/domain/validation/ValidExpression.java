package de.vitagroup.num.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = ExpressionValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface ValidExpression {

    String message() default "Phenotype expression must be a valid one";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

