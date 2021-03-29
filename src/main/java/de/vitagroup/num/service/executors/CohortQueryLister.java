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

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

      cohortGroup
          .getChildren()
          .forEach(
              subGroup -> queries.addAll(listCohortGroup(subGroup)));

      return queries;

    } else if (cohortGroup.getType() == Type.PHENOTYPE && cohortGroup.getPhenotype() != null) {

      return listPhenotype(cohortGroup.getPhenotype());
    }
    return Collections.emptyList();
  }

  @NotNull
  private List<String> listPhenotype(@NotNull Phenotype phenotype) {
    if (phenotype.getQuery() == null) {
      return Collections.emptyList();
    }
    return listExpression(phenotype.getQuery());
  }

  @NotNull
  private List<String> listExpression(@NotNull Expression expression) {
    if (expression instanceof GroupExpression) {
      List<String> queries = new ArrayList<>();
      GroupExpression groupExpression = (GroupExpression) expression;
      groupExpression
          .getChildren()
          .forEach(
              subGroup -> queries.addAll(listExpression(subGroup)));

      return queries;

    } else if (expression instanceof AqlExpression) {
      AqlExpression aqlExpression = (AqlExpression) expression;
      return List.of(aqlExpression.getAql().getQuery());
    }
    return Collections.emptyList();
  }
}
