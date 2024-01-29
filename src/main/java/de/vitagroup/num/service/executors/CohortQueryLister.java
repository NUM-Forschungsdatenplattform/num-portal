package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.model.Cohort;
import de.vitagroup.num.domain.model.CohortAql;
import de.vitagroup.num.domain.model.CohortGroup;
import de.vitagroup.num.domain.model.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class CohortQueryLister {

  @NotNull
  public List<String> list(@NotNull Cohort cohort) {
    if (cohort.getCohortGroup() == null) {
      return Collections.emptyList();
    }
    return listCohortGroup(cohort.getCohortGroup());
  }

  @NotNull
  private List<String> listCohortGroup(@NotNull CohortGroup cohortGroup) {
    List<String> queries = new ArrayList<>();
    if (cohortGroup.getType() == Type.GROUP) {

      cohortGroup.getChildren().forEach(subGroup -> queries.addAll(listCohortGroup(subGroup)));

      return queries;

    } else if (cohortGroup.getType() == Type.AQL && cohortGroup.getQuery() != null) {

      return listAql(cohortGroup.getQuery());
    }
    return Collections.emptyList();
  }

  @NotNull
  private List<String> listAql(@NotNull CohortAql cohortAql) {
    if (StringUtils.isNotEmpty(cohortAql.getQuery())) {
      return List.of(cohortAql.getQuery());
    } else {
      return Collections.emptyList();
    }
  }
}
