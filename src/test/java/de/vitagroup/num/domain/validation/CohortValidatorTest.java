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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import de.vitagroup.num.domain.*;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

public class CohortValidatorTest {

  private Validator validator;

  @Before
  public void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void correctlyValidatesSimpleCohort() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(2L).build();

    CohortGroupDto andCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();
    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").studyId(1L).cohortGroup(andCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void supportsOrOperation() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(2L).build();
    CohortGroupDto third = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(3L).build();

    CohortGroupDto orCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.OR)
            .children(List.of(first, second, third))
            .build();
    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").studyId(1L).cohortGroup(orCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void correctlyValidatesNotOperation() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(2L).build();
    CohortGroupDto third = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(3L).build();

    CohortGroupDto notCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.NOT)
            .children(List.of(first, second, third))
            .build();
    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").studyId(1L).cohortGroup(notCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getMessage(), is("Invalid cohort group"));
  }

  @Test
  public void correctlyValidatesOrOperation() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();

    CohortGroupDto singlePhenotypeOrGroup =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.OR)
            .children(List.of(first))
            .build();

    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").studyId(1L).cohortGroup(singlePhenotypeOrGroup).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void correctlyValidatesCohortName() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto notCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.NOT)
            .children(List.of(first))
            .build();
    CohortDto cohortDto = CohortDto.builder().studyId(1L).cohortGroup(notCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(2));

    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Cohort name should not be blank")),
        is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Cohort name is mandatory")),
        is(true));
  }

  @Test
  public void correctlyValidatesCohortStudy() {
    CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto notCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.NOT)
            .children(List.of(first))
            .build();
    CohortDto cohortDto = CohortDto.builder().name("Name").cohortGroup(notCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getMessage(), is("Id of the study is mandatory"));
  }

  @Test
  public void correctlyValidatesCohortGroupType() {
    CohortGroupDto notCohort = CohortGroupDto.builder().operator(Operator.NOT).build();
    CohortDto cohortDto =
        CohortDto.builder().name("Name").studyId(1L).cohortGroup(notCohort).build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getMessage(), is("Invalid cohort group"));
  }

  @Test
  public void correctlyValidatesCohortGroup() {
    CohortDto cohortDto = CohortDto.builder().build();
    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(5));

    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Cohort name should not be blank")),
        is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Id of the study is mandatory")),
        is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Invalid cohort group")), is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Cohort group is mandatory")),
        is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Cohort name is mandatory")),
        is(true));
  }

  @Test
  public void shouldCorrectlyValidateCohortParameters() {
    CohortGroupDto simpleCohort =
        CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).parameters(null).build();
    CohortDto cohortDto =
        CohortDto.builder()
            .name("Cohort name")
            .description("Cohort parameters are optional")
            .studyId(1L)
            .cohortGroup(simpleCohort)
            .build();

    Set<ConstraintViolation<CohortDto>> violations = validator.validate(cohortDto);

    assertThat(violations.isEmpty(), is(true));
  }
}
