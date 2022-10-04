package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.criteria.*;
import java.util.*;

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
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ORGANIZATION.name());
        ProjectSpecification ps = ProjectSpecification.builder()
                .filter(filter)
                .roles(Arrays.asList(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void roleResearcherWithFilterSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Mockito.when(coordinator.join("organization", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Join reasearcher = Mockito.mock(Join.class);
        Mockito.when(root.join("researchers", JoinType.LEFT)).thenReturn(reasearcher);
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Mockito.when(coordinator.get("userId")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(coordinator.get("userId").in(Mockito.anyCollection())).thenReturn(Mockito.mock(Predicate.class));
        Mockito.when(criteriaBuilder.upper(root.get("name"))).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "search me");
        Set<String> usersUUID = new HashSet<>();
        usersUUID.add("user-id-1");
        usersUUID.add("user-id-2");
        ProjectSpecification ps = ProjectSpecification.builder()
                .filter(filter)
                .roles(Arrays.asList(Roles.RESEARCHER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .ownersUUID(usersUUID)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("status");
        Mockito.verify(reasearcher, Mockito.times(1)).get("userId");
    }

    @Test
    public void getOwnedSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Mockito.when(root.get("status")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(coordinator.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.OWNED.name());
        ProjectSpecification projectSpecification = ProjectSpecification.builder()
                .filter(filter)
                .loggedInUserId("userId")
                .roles(Arrays.asList(Roles.STUDY_COORDINATOR))
                .build();
        projectSpecification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void getArchivedSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Mockito.when(root.get("status")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(coordinator.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ARCHIVED.name());
        ProjectSpecification projectSpecification = ProjectSpecification.builder()
                .filter(filter)
                .loggedInUserId("userId")
                .roles(Arrays.asList(Roles.STUDY_COORDINATOR))
                .build();
        projectSpecification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldHandleNotSupportedFilterType() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Mockito.when(coordinator.join("organization", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Mockito.when(root.get("status")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ALL.name());
        ProjectSpecification projectSpecification = ProjectSpecification.builder()
                .filter(filter)
                .loggedInUserId("userId")
                .roles(Arrays.asList(Roles.STUDY_COORDINATOR))
                .build();
        projectSpecification.toPredicate(root, query, criteriaBuilder);
    }
}
