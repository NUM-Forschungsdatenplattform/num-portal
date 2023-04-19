package de.vitagroup.num.domain.validation;

import de.vitagroup.num.domain.*;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import org.apache.commons.collections4.CollectionUtils;

import jakarta.validation.*;
import java.util.ArrayDeque;
import java.util.Queue;

public class CohortValidator implements ConstraintValidator<ValidCohort, CohortGroupDto> {

  @Override
  public boolean isValid(
      CohortGroupDto cohortGroupDto, ConstraintValidatorContext constraintValidatorContext) {
    return isValid(cohortGroupDto);
  }

  private boolean isValid(CohortGroupDto cohortGroupDto) {
    if (cohortGroupDto == null || cohortGroupDto.getType() == null) {
      return false;
    }

    Queue<CohortGroupDto> queue = new ArrayDeque<>();
    queue.add(cohortGroupDto);

    while (!queue.isEmpty()) {
      CohortGroupDto current = queue.remove();

      if (current.getType().equals(Type.GROUP) && isInvalidGroup(current)) {
        return false;
      } else if (current.getType().equals(Type.AQL) && isInvalidAql(current)) {
        return false;
      } else if (current.getType().equals(Type.GROUP)) {
        queue.addAll(current.getChildren());
      }
    }
    return true;
  }

  private boolean isInvalidGroup(CohortGroupDto cohortGroup) {
    // Group children cannot be empty nor group operator can be null
    if (CollectionUtils.isEmpty(cohortGroup.getChildren()) || cohortGroup.getOperator() == null) {
      return true;
    }

    // NOT operator is unary - group cannot have more than one child
    if ((cohortGroup.getOperator().equals(Operator.NOT) && cohortGroup.getChildren().size() > 1)) {
      return true;
    }

    return false;
  }

  private boolean isInvalidAql(CohortGroupDto cohortGroup) {
    return (CollectionUtils.isNotEmpty(cohortGroup.getChildren())
        || cohortGroup.getQuery() == null
        || cohortGroup.getQuery().getId() == null
        || cohortGroup.getQuery().getId() == 0);
  }
}
