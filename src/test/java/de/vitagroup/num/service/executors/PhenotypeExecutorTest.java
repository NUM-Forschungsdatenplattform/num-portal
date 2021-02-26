package de.vitagroup.num.service.executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeExecutorTest {

  private final String PHENOTYPE_NAME = "Phenotype name";
  private final String AQL_NAME = "AQL query name";
  private final String AQL_QUERY = "SELECT A ... FROM E ... WHERE ...";
  @Spy private SetOperationsService setOperations;
  @Mock private EhrBaseService ehrBaseService;
  @InjectMocks private PhenotypeExecutor phenotypeExecutor;

  @Before
  public void setup() {
    when(ehrBaseService.getAllPatientIds())
        .thenReturn(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
  }

  @Test
  public void shouldCorrectlyExecuteAndPhenotype() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "5", "10"));
    when(ehrBaseService.retrieveEligiblePatientIds(aql2)).thenReturn(Set.of("1", "4", "5", "6", "10"));

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.AND)
            .children(Arrays.asList(aqlExpression1, aqlExpression2))
            .build();

    Phenotype phenotype = Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(query).build();

    Set<String> result = phenotypeExecutor.execute(phenotype);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "5", "10")), is(true));
  }

  @Test
  public void shouldCorrectlyExecuteSingleAqlAndOperation() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "5", "10"));

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.AND)
            .children(Arrays.asList(aqlExpression1))
            .build();

    Phenotype phenotype = Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(query).build();

    Set<String> result = phenotypeExecutor.execute(phenotype);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "5", "10")), is(true));
  }

  @Test
  public void shouldCorrectlyExecuteOrPhenotype() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "2", "3"));
    when(ehrBaseService.retrieveEligiblePatientIds(aql2)).thenReturn(Set.of("3", "4", "5", "6", "7"));

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.OR)
            .children(Arrays.asList(aqlExpression1, aqlExpression2))
            .build();

    Phenotype phenotype = Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(query).build();

    Set<String> result = phenotypeExecutor.execute(phenotype);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "3", "4", "5", "6", "7")), is(true));
  }

  @Test
  public void shouldCorrectlyExecuteOrNotPhenotype() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "4"));
    when(ehrBaseService.retrieveEligiblePatientIds(aql2)).thenReturn(Set.of("1", "2", "3"));

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression notAql = AqlExpression.builder().aql(aql2).build();

    GroupExpression notNode =
        GroupExpression.builder()
            .operator(Operator.NOT)
            .children(Collections.singletonList(notAql))
            .build();
    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.OR)
            .children(Arrays.asList(aqlExpression1, notNode))
            .build();

    Phenotype phenotype = Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(query).build();

    Set<String> result = phenotypeExecutor.execute(phenotype);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "4", "5", "6", "7", "8", "9", "10")), is(true));
  }

  @Test
  public void shouldCorrectlyExecuteAndNotPhenotype() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "4"));
    when(ehrBaseService.retrieveEligiblePatientIds(aql2)).thenReturn(Set.of("1", "2", "3"));

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression notAql = AqlExpression.builder().aql(aql2).build();

    GroupExpression notNode =
        GroupExpression.builder()
            .operator(Operator.NOT)
            .children(Collections.singletonList(notAql))
            .build();
    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.AND)
            .children(Arrays.asList(aqlExpression1, notNode))
            .build();

    Phenotype phenotype = Phenotype.builder().id(1L).name(PHENOTYPE_NAME).query(query).build();

    Set<String> result = phenotypeExecutor.execute(phenotype);

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("4")), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldHandleNullPhenotype() {
    phenotypeExecutor.execute(null);
  }
}
