package de.vitagroup.num.domain.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.vitagroup.num.converter.CohortConverter;
import de.vitagroup.num.domain.*;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.service.StudyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortDtoTest {

  @Mock private PhenotypeService phenotypeService;

  @Mock private StudyService studyService;

  @Spy private ModelMapper modelMapper;

  @InjectMocks private CohortConverter converter;

  @Before
  public void setup() {
    Study study =
        Study.builder().id(1L).name("Study name").description("Study description").build();

    Aql aql1 =
        Aql.builder()
            .id(1L)
            .name("AQL query name 1 ")
            .query("SELECT A1 ... FROM E1... WHERE ...")
            .build();
    Aql aql2 =
        Aql.builder()
            .id(2L)
            .name("AQL query name 2")
            .query("SELECT A2 ... FROM E2... WHERE ...")
            .build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    Phenotype phenotype1 =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Phenotype description")
            .query(aqlExpression1)
            .build();
    Phenotype phenotype2 =
        Phenotype.builder()
            .id(2L)
            .name("Phenotype name")
            .description("Phenotype description")
            .query(aqlExpression2)
            .build();

    when(studyService.getStudyById(any())).thenReturn(Optional.of(study));
    when(phenotypeService.getPhenotypeById(1L)).thenReturn(Optional.of(phenotype1));
    when(phenotypeService.getPhenotypeById(2L)).thenReturn(Optional.of(phenotype2));
  }

  @Test
  public void shouldCorrectlyConvertCohortToCohortDto() {
    Study study =
        Study.builder().id(1L).name("Study name").description("Study description").build();

    Aql aql1 =
        Aql.builder()
            .id(1L)
            .name("AQL query name 1 ")
            .query("SELECT A1 ... FROM E1... WHERE ...")
            .build();
    Aql aql2 =
        Aql.builder()
            .id(2L)
            .name("AQL query name 2")
            .query("SELECT A2 ... FROM E2... WHERE ...")
            .build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    Phenotype phenotype1 =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Phenotype description")
            .query(aqlExpression1)
            .build();
    Phenotype phenotype2 =
        Phenotype.builder()
            .id(2L)
            .name("Phenotype name")
            .description("Phenotype description")
            .query(aqlExpression2)
            .build();

    CohortGroup first = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype1).build();
    CohortGroup second = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype2).build();

    CohortGroup andCohort =
        CohortGroup.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(Set.of(first, second))
            .build();

    Cohort cohort =
        Cohort.builder().name("Cohort name").study(study).cohortGroup(andCohort).build();
    CohortDto cohortDto = converter.convertToDto(cohort);

    assertThat(cohortDto.getName(), notNullValue());
    assertThat(cohortDto.getName(), is("Cohort name"));

    assertThat(cohortDto.getStudyId(), is(study.getId()));

    assertThat(cohortDto.getCohortGroup(), notNullValue());
    assertThat(cohortDto.getCohortGroup().getType(), is(Type.GROUP));
    assertThat(cohortDto.getCohortGroup().getOperator(), is(Operator.AND));
    assertThat(cohortDto.getCohortGroup().getChildren().size(), is(2));

    assertThat(cohortDto.getCohortGroup().getChildren().get(0).getType(), is(Type.PHENOTYPE));
    assertThat(cohortDto.getCohortGroup().getChildren().get(1).getType(), is(Type.PHENOTYPE));

    assertThat(
        cohortDto.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotypeId() == 1),
        is(true));
    assertThat(
        cohortDto.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotypeId() == 2),
        is(true));
  }

  @Test
  public void shouldCorrectlyConvertCohortDtoToCohort() {
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
    Cohort cohort = converter.convertToEntity(cohortDto);

    assertThat(cohort, notNullValue());
    assertThat(cohort.getStudy(), notNullValue());
    assertThat(cohort.getStudy().getId(), is(1L));
    assertThat(cohort.getStudy().getName(), is("Study name"));
    assertThat(cohort.getCohortGroup().getOperator(), is(Operator.AND));
    assertThat(cohort.getCohortGroup().getType(), is(Type.GROUP));
    assertThat(cohort.getCohortGroup().getChildren().size(), is(2));

    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 1),
        is(true));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 2),
        is(true));

    assertThat(
        cohort.getCohortGroup().getChildren().stream()
            .allMatch(c -> c.getPhenotype().getQuery() instanceof AqlExpression),
        is(true));
  }

  @Test
  public void shouldCorrectlyConvertCohortDtoParameters() {

    CohortGroupDto first =
        CohortGroupDto.builder()
            .type(Type.PHENOTYPE)
            .phenotypeId(1L)
            .parameters(Map.of("param1", "value1", "param2", "value2"))
            .build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(2L).build();

    CohortGroupDto andCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").studyId(1L).cohortGroup(andCohort).build();
    Cohort cohort = converter.convertToEntity(cohortDto);

    assertThat(cohort, notNullValue());
    assertThat(cohort.getStudy(), notNullValue());
    assertThat(cohort.getStudy().getId(), is(1L));
    assertThat(cohort.getStudy().getName(), is("Study name"));
    assertThat(cohort.getCohortGroup().getOperator(), is(Operator.AND));
    assertThat(cohort.getCohortGroup().getType(), is(Type.GROUP));

    assertThat(cohort.getCohortGroup().getParameters(), nullValue());

    assertThat(cohort.getCohortGroup().getChildren().size(), is(2));

    assertThat(
        cohort.getCohortGroup().getChildren().stream()
            .anyMatch(
                c ->
                    c.getParameters() != null
                        && c.getParameters().containsKey("param1")
                        && c.getParameters().containsKey("param2")),
        is(true));

    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 1),
        is(true));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 2),
        is(true));

    assertThat(
        cohort.getCohortGroup().getChildren().stream()
            .allMatch(c -> c.getPhenotype().getQuery() instanceof AqlExpression),
        is(true));
  }

  @Test
  public void shouldNotCopyIdsFromCohortDtoToCohort() {
    CohortGroupDto first =
        CohortGroupDto.builder().id(1L).type(Type.PHENOTYPE).phenotypeId(1L).build();
    CohortGroupDto second =
        CohortGroupDto.builder().id(2L).type(Type.PHENOTYPE).phenotypeId(2L).build();

    CohortGroupDto andCohort =
        CohortGroupDto.builder()
            .id(12L)
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    CohortDto cohortDto =
        CohortDto.builder().id(16L).name("Cohort name").studyId(1L).cohortGroup(andCohort).build();

    Cohort cohort = converter.convertToEntity(cohortDto);

    assertThat(cohort, notNullValue());
    assertThat(cohort.getId(), is(nullValue()));
    assertThat(cohort.getCohortGroup().getId(), is(nullValue()));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getId() == null), is(true));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getId() == null), is(true));
  }

  @Test
  public void shouldCopyIdsFromCohortToCohortDto() {

    CohortGroup first = CohortGroup.builder().id(1L).type(Type.PHENOTYPE).phenotype(null).build();
    CohortGroup second = CohortGroup.builder().id(2L).type(Type.PHENOTYPE).phenotype(null).build();

    CohortGroup andCohort =
        CohortGroup.builder()
            .id(12L)
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(Set.of(first, second))
            .build();

    Cohort cohort =
        Cohort.builder().name("Cohort name").id(17L).study(null).cohortGroup(andCohort).build();
    CohortDto cohortDto = converter.convertToDto(cohort);

    assertThat(cohortDto, notNullValue());
    assertThat(cohortDto.getId(), is(cohort.getId()));
    assertThat(cohortDto.getCohortGroup().getId(), is(andCohort.getId()));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getId() == first.getId()),
        is(true));
    assertThat(
        cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getId() == second.getId()),
        is(true));
  }
}
