package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortAql;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.service.ProjectService;
import java.util.List;
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

  @Mock
  private AqlService aqlService;

  @Mock
  private ProjectService projectService;

  @Spy
  private ModelMapper modelMapper;

  @InjectMocks
  private CohortMapper cohortMapper;

  private final String Q1 = "SELECT A1 ... FROM E1... WHERE ...";
  private final String Q2 = "SELECT A2 ... FROM E1... WHERE ...";
  private final String NAME1 = "AQL query name 1";
  private final String NAME2 = "AQL query name 2";

  @Before
  public void setup() {
    cohortMapper.initialize();
  }

  @Test
  public void shouldCorrectlyConvertCohortToCohortDto() {
    CohortAql cohortAql1 = CohortAql.builder().id(1L).name(NAME1).query(Q1).build();
    CohortAql cohortAql2 = CohortAql.builder().id(2L).name(NAME2).query(Q2).build();

    CohortGroup first = CohortGroup.builder().type(Type.AQL).query(cohortAql1).build();
    CohortGroup second = CohortGroup.builder().type(Type.AQL).query(cohortAql2).build();

    CohortGroup andCohort =
        CohortGroup.builder()
            .id(12L)
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    Cohort cohort =
        Cohort.builder().name("Cohort name").id(17L).project(null).cohortGroup(andCohort).build();

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
    assertThat(
        cohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getQuery().equals(Q1)),
        is(true));
    assertThat(
        cohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getQuery().equals(Q2)),
        is(true));
  }
}
