package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.service.StudyService;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class CohortMapperTest {

  @Mock private PhenotypeService phenotypeService;

  @Mock private StudyService studyService;

  @Spy private ModelMapper modelMapper;

  @InjectMocks private CohortMapper cohortMapper;

  @Before
  public void setup() {
    cohortMapper.initialize();

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
    CohortDto cohortDto = cohortMapper.convertToDto(cohort);

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
    CohortDto cohortDto = cohortMapper.convertToDto(cohort);

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
