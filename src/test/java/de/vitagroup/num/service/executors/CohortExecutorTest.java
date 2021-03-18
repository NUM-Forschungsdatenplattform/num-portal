package de.vitagroup.num.service.executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CohortExecutorTest {

  private final String COHORT_NAME = "Phenotype name";
  private final String PHENOTYPE_NAME = "Phenotype name";
  private final String AQL_NAME = "AQL query name";
  private final String AQL_QUERY = "SELECT A ... FROM E ... WHERE ...";
  @Spy private SetOperationsService setOperations;
  @Mock private EhrBaseService ehrBaseService;
  @Mock private PhenotypeExecutor phenotypeExecutor;
  @InjectMocks private CohortExecutor cohortExecutor;

  @Before
  public void setup() {
    when(ehrBaseService.getAllPatientIds())
        .thenReturn(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
  }

  @Test
  public void shouldCorrectlyExecuteAndCohort() {

    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    Phenotype phenotype1 =
        Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(aqlExpression1).build();
    Phenotype phenotype2 =
        Phenotype.builder().id(2L).name(PHENOTYPE_NAME).query(aqlExpression2).build();

    when(phenotypeExecutor.execute(phenotype1, null)).thenReturn(Set.of("1", "2", "5", "10"));
    when(phenotypeExecutor.execute(phenotype2, null)).thenReturn(Set.of("1", "2", "4", "5", "6", "10"));

    CohortGroup first = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype1).build();
    CohortGroup second = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype2).build();

    CohortGroup andCohort =
        CohortGroup.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(Set.of(first, second))
            .build();

    Cohort cohort = Cohort.builder().name(COHORT_NAME).cohortGroup(andCohort).build();

    Set<String> result = cohortExecutor.execute(cohort);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "5", "10")), is(true));
  }

  @Test
  public void shouldCorrectlyExecuteOrCohort() {

    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    Phenotype phenotype1 =
        Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(aqlExpression1).build();
    Phenotype phenotype2 =
        Phenotype.builder().id(2L).name(PHENOTYPE_NAME).query(aqlExpression2).build();

    when(phenotypeExecutor.execute(phenotype1, null)).thenReturn(Set.of("1", "2", "5", "10"));
    when(phenotypeExecutor.execute(phenotype2, null))
        .thenReturn(Set.of("4", "5", "6", "7", "8", "9", "10"));

    CohortGroup first = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype1).build();
    CohortGroup second = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype2).build();

    CohortGroup orCohort =
        CohortGroup.builder()
            .type(Type.GROUP)
            .operator(Operator.OR)
            .children(Set.of(first, second))
            .build();

    Cohort cohort = Cohort.builder().name(COHORT_NAME).cohortGroup(orCohort).build();

    Set<String> result = cohortExecutor.execute(cohort);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "4", "5", "6", "7", "8", "9", "10")), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldHandleNullCohort() {
    cohortExecutor.execute(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldHandleNullCohortGroup() {
    Cohort cohort = Cohort.builder().name(COHORT_NAME).cohortGroup(null).build();
    cohortExecutor.execute(cohort);
  }
}
