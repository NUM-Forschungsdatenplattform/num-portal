package de.vitagroup.num.domain.validation;

import de.vitagroup.num.domain.*;
import de.vitagroup.num.domain.dtos.CohortGroupDto;

import javax.validation.*;
import java.util.ArrayDeque;
import java.util.Queue;

public class CohortValidator implements ConstraintValidator<ValidCohort, CohortGroupDto> {

    @Override
    public boolean isValid(CohortGroupDto cohortGroupDto, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(cohortGroupDto);
    }

    private boolean isValid(CohortGroupDto cohortGroupDto) {
        if (cohortGroupDto == null) {
            return false;
        }

        Queue<CohortGroupDto> queue = new ArrayDeque<>();
        queue.add(cohortGroupDto);

        while (!queue.isEmpty()) {
            CohortGroupDto current = queue.remove();

            if (current.getType().equals(Type.GROUP) && isInvalidGroup(current)) {
                return false;
            } else if (current.getType().equals(Type.PHENOTYPE) && isInvalidPhenotype(current)) {
                return false;
            } else if (current.getType().equals(Type.GROUP)) {
                queue.addAll(current.getChildren());
            }
        }
        return true;
    }

    private boolean isInvalidGroup(CohortGroupDto cohortGroup) {
        if (cohortGroup.getChildren() == null || cohortGroup.getChildren().isEmpty() ||
                cohortGroup.getOperator() == null || (cohortGroup.getOperator().equals(Operator.NOT) && cohortGroup.getChildren().size() > 1)) {
            return true;
        }
        return false;
    }

    private boolean isInvalidPhenotype(CohortGroupDto cohortGroup) {
        if ((cohortGroup.getChildren() != null && cohortGroup.getChildren().size() > 0) || cohortGroup.getPhenotypeId() == 0) {
            return true;
        }
        return false;
    }
}
