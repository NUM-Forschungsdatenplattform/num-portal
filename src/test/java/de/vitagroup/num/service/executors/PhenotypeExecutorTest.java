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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
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
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

  @Captor ArgumentCaptor<Aql> aqlArgumentCaptor;

  private static final String QUERY_WITH_PARAMS =
      "SELECT"
          + "  o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Systolic__magnitude,"
          + "  o0/data[at0001]/events[at0002]/data[at0003]/items[at0005]/value/magnitude as Diastolic__magnitude,"
          + "  o1/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude as Temperatur__magnitude"
          + " FROM"
          + "  EHR e"
          + "  contains (COMPOSITION c2[openEHR-EHR-COMPOSITION.sample_encounter.v1]"
          + "  contains OBSERVATION o0[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]"
          + "  and COMPOSITION c3[openEHR-EHR-COMPOSITION.report.v1]"
          + "  contains OBSERVATION o1[openEHR-EHR-OBSERVATION.body_temperature.v2])"
          + " WHERE"
          + "  o0/data[at0001]/events[at0002]/data[at0003]/data[at0003]/items[at0004]/value/magnitude > $systolicCriteria  "
          + "and o0/data[at0001]/events[at0002]/data[at0003]/items[at0005]/value/magnitude = $otherCriteria "
          + "and o1/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude = $systolicCriteria";

  @Before
  public void setup() {
    when(ehrBaseService.getAllPatientIds())
        .thenReturn(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
  }

  @Test
  public void shouldCorrectlyApplyParameters() {
    Phenotype phenotype =
        Phenotype.builder()
            .id(5L)
            .query(
                AqlExpression.builder().aql(Aql.builder().query(QUERY_WITH_PARAMS).build()).build())
            .build();

    phenotypeExecutor.execute(phenotype, Map.of("systolicCriteria", "120", "otherCriteria", 45));
    Mockito.verify(ehrBaseService, times(1))
        .retrieveEligiblePatientIds(aqlArgumentCaptor.capture());

    assertThat(aqlArgumentCaptor.getAllValues(), notNullValue());
    assertThat(aqlArgumentCaptor.getAllValues().size(), is(1));

    Aql aql = aqlArgumentCaptor.getAllValues().get(0);

    assertThat(aql, notNullValue());
    assertFalse(aql.getQuery().contains("systolicCriteria"));
    assertFalse(aql.getQuery().contains("otherCriteria"));
    assertTrue(aql.getQuery().contains("120"));
    assertTrue(aql.getQuery().contains("45"));
  }

  @Test
  public void shouldCorrectlyExecuteAndPhenotype() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    when(ehrBaseService.retrieveEligiblePatientIds(aql1)).thenReturn(Set.of("1", "5", "10"));
    when(ehrBaseService.retrieveEligiblePatientIds(aql2))
        .thenReturn(Set.of("1", "4", "5", "6", "10"));

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
    when(ehrBaseService.retrieveEligiblePatientIds(aql2))
        .thenReturn(Set.of("3", "4", "5", "6", "7"));

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
