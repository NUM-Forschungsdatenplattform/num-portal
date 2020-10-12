package de.vitagroup.num.domain.validation;

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Operator;

import javax.validation.*;
import java.util.ArrayDeque;
import java.util.Queue;

public class ExpressionValidator implements ConstraintValidator<ValidExpression, Expression> {

    @Override
    public boolean isValid(Expression expression, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(expression);
    }

    private boolean isValid(Expression expression) {
        if (expression == null) {
            return false;
        }

        Queue<Expression> queue = new ArrayDeque<>();
        queue.add(expression);

        while (!queue.isEmpty()) {
            Expression current = queue.remove();

            if (current instanceof GroupExpression && isInvalidGroupExpression(((GroupExpression) current))) {
                return false;
            } else if (current instanceof AqlExpression && isInvalidAqlExpression((AqlExpression) current)) {
                return false;
            } else if (current instanceof GroupExpression) {
                queue.addAll(((GroupExpression) current).getChildren());
            }
        }
        return true;
    }

    private boolean isInvalidAqlExpression(AqlExpression node) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        return !validator.validate(node.getAql()).isEmpty();
    }

    private boolean isInvalidGroupExpression(GroupExpression node) {
        if (node.getChildren() == null || node.getChildren().isEmpty() || (node.getOperator().equals(Operator.NOT) && node.getChildren().size() > 1)){
            return true;
        }
        return false;
    }

}
