package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.service.MockEhrService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.vitagroup.num.service.exception.IllegalArgumentException;

@Slf4j
@Service
@AllArgsConstructor
public class CohortExecutor {

  private final SetOperationsService setOperations;
  private final PhenotypeExecutor phenotypeExecutor;
  private final MockEhrService mockEhrService;

  public Set<String> execute(Cohort cohort) {

    if (cohort == null || cohort.getCohortGroup() == null) {
      throw new IllegalArgumentException("Cannot execute an empty cohort");
    }

    return execute(cohort.getCohortGroup());
  }

  private Set<String> execute(CohortGroup cohortGroup) {
    if (cohortGroup.getType() == Type.GROUP) {

      List<Set<String>> sets =
          cohortGroup.getChildren().stream().map(this::execute).collect(Collectors.toList());

      return setOperations.apply(cohortGroup.getOperator(), sets, getAllPatientIds());

    } else if (cohortGroup.getType() == Type.PHENOTYPE) {

      return phenotypeExecutor.execute(cohortGroup.getPhenotype());
    }

    return SetUtils.emptySet();
  }

  // TODO: implement call to the service responsible for querying open ehr for all patient ids;
  // service should cache patient ids per cohort execution
  private Set<String> getAllPatientIds() {
    return mockEhrService.getAllPatientIds();
  }
}
