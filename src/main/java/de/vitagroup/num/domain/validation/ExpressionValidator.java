/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.domain.validation;

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Operator;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.*;
import java.util.ArrayDeque;
import java.util.Queue;

public class ExpressionValidator implements ConstraintValidator<ValidExpression, Expression> {

  @Override
  public boolean isValid(
      Expression expression, ConstraintValidatorContext constraintValidatorContext) {
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

      if (current instanceof GroupExpression
          && isInvalidGroupExpression(((GroupExpression) current))) {
        return false;
      } else if (current instanceof AqlExpression
          && isInvalidAqlExpression((AqlExpression) current)) {
        return false;
      } else if (current instanceof GroupExpression) {
        queue.addAll(((GroupExpression) current).getChildren());
      }
    }
    return true;
  }

  private boolean isInvalidAqlExpression(AqlExpression node) {
    if (node.getAql() == null) {
      return true;
    }

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    return !validator.validate(node.getAql()).isEmpty();
  }

  private boolean isInvalidGroupExpression(GroupExpression node) {
    // Group children cannot be empty
    if (CollectionUtils.isEmpty(node.getChildren())) {
      return true;
    }

    // NOT operator is unary - group cannot have more than one child
    if ((node.getOperator().equals(Operator.NOT) && node.getChildren().size() > 1)) {
      return true;
    }

    return false;
  }
}
