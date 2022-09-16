package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Roles;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSpecificationTest {

    @Mock
    private Root<Project> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;


    @Test
    public void roleCoordinatorSpecificationTest() {
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        ProjectSpecification ps = ProjectSpecification
                .builder()
                .roles(Arrays.asList(Roles.STUDY_COORDINATOR))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void roleApproverWithFilterSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Mockito.when(coordinator.join("organization", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Map<String, String> filter = new HashMap<>();
        filter.put("type", ProjectFilterType.ORGANIZATION.name());
        ProjectSpecification ps = ProjectSpecification.builder()
                .filter(filter)
                .roles(Arrays.asList(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

}
