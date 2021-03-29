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

package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CohortExecutor {

  private final SetOperationsService setOperations;
  private final PhenotypeExecutor phenotypeExecutor;
  private final EhrBaseService ehrBaseService;

  public Set<String> execute(Cohort cohort) {

    if (cohort == null || cohort.getCohortGroup() == null) {
      throw new IllegalArgumentException("Cannot execute an empty cohort");
    }

    return executeGroup(cohort.getCohortGroup(), cohort.getCohortGroup().getParameters());
  }

  public Set<String> executeGroup(CohortGroup cohortGroup, Map<String, Object> parameters) {
    if (cohortGroup.getType() == Type.GROUP) {

      List<Set<String>> sets =
          cohortGroup.getChildren().stream()
              .map(e -> executeGroup(e, parameters))
              .collect(Collectors.toList());

      return setOperations.apply(
          cohortGroup.getOperator(), sets, ehrBaseService.getAllPatientIds());

    } else if (cohortGroup.getType() == Type.PHENOTYPE) {

      return phenotypeExecutor.execute(cohortGroup.getPhenotype(), parameters);
    }

    return SetUtils.emptySet();
  }
}
