package org.highmed.numportal.service.executors;

import org.highmed.numportal.service.executors.CohortQueryLister;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.numportal.domain.model.Cohort;
import org.highmed.numportal.domain.model.CohortAql;
import org.highmed.numportal.domain.model.CohortGroup;
import org.highmed.numportal.domain.model.Type;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CohortQueryListerTest {

    @InjectMocks
    private CohortQueryLister cohortQueryLister;

    @Test
    public void shouldReturnEmptyListWhenGroupIsNull() {
        List<String> result = cohortQueryLister.list(Cohort.builder()
                .name("without group")
                .build());
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnListOfQueriesForGroup() {
        final String AQL_QUERY = "SELECT a FROM Ehr e";
        CohortGroup cohortGroup = CohortGroup.builder()
                .type(Type.GROUP)
                .children(List.of(CohortGroup.builder()
                        .type(Type.AQL)
                        .query(CohortAql.builder()
                                .query(AQL_QUERY)
                                .build())
                        .build()))
                .build();
        List<String> result = cohortQueryLister.list(Cohort.builder()
                .name("cohort of type group")
                .cohortGroup(cohortGroup)
                .build());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(AQL_QUERY, result.get(0));
    }

    @Test
    public void shouldReturnEmptyListWhenQueryIsMissing() {
        CohortGroup cohortGroup = CohortGroup.builder()
                .type(Type.AQL)
                .query(CohortAql.builder().build())
                .build();
        List<String> result = cohortQueryLister.list(Cohort.builder()
                .name("cohort of type aql")
                .cohortGroup(cohortGroup)
                .build());
        Assert.assertTrue(result.isEmpty());
    }
}
