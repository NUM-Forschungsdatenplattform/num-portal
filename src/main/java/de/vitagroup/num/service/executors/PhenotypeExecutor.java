package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PhenotypeExecutor {

  private final SetOperationsService setOperations;
  private final EhrBaseService ehrBaseService;

  public Set<String> execute(Phenotype phenotype) {

    if (phenotype == null || phenotype.getQuery() == null) {
      throw new IllegalArgumentException("Cannot execute an empty phenotype");
    }

    return execute(phenotype.getQuery());
  }

  private Set<String> execute(Expression expression) {
    Set<String> all = ehrBaseService.getAllPatientIds();

    if (expression instanceof GroupExpression) {
      GroupExpression groupExpression = (GroupExpression) expression;
      List<Set<String>> sets =
          groupExpression.getChildren().stream().map(this::execute).collect(Collectors.toList());

      return setOperations.apply(groupExpression.getOperator(), sets, all);

    } else if (expression instanceof AqlExpression) {

      AqlExpression aqlExpression = (AqlExpression) expression;

      return ehrBaseService.retrieveEligiblePatientIds(aqlExpression.getAql());
    }
    return SetUtils.emptySet();
  }
}
