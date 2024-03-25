package org.highmed.domain.specification;

import jakarta.persistence.criteria.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.highmed.domain.dto.Language;
import org.highmed.domain.dto.SearchFilter;
import org.highmed.domain.model.Project;
import org.highmed.domain.model.ProjectStatus;
import org.highmed.domain.model.Roles;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSpecificationTest {

    @Mock
    private Root<Project> root;

    @Mock
    private CriteriaBuilder criteriaBuilder;


    @Test
    public void roleCoordinatorSpecificationTest() {
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        ProjectSpecification ps = ProjectSpecification
                .builder()
                .roles(List.of(Roles.STUDY_COORDINATOR))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .build();
        ps.toPredicate(root, criteriaBuilder);
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
        filter.put("type", SearchFilter.ORGANIZATION.name());
        ProjectSpecification ps = ProjectSpecification.builder()
                .filter(filter)
                .roles(List.of(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .build();
        ps.toPredicate(root, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void roleResearcherWithFilterSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Join reasearcher = Mockito.mock(Join.class);
        Mockito.when(root.join("researchers", JoinType.LEFT)).thenReturn(reasearcher);
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Mockito.when(coordinator.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("search", "search me");
        filter.put("status", "PUBLISHED");
        Set<String> usersUUID = new HashSet<>();
        usersUUID.add("user-id-1");
        usersUUID.add("user-id-2");
        Mockito.when(coordinator.get("userId").in(usersUUID)).thenReturn(Mockito.mock(Predicate.class));
        ProjectSpecification ps = ProjectSpecification.builder()
                .filter(filter)
                .roles(List.of(Roles.RESEARCHER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .ownersUUID(usersUUID)
                .build();
        ps.toPredicate(root, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
        Mockito.verify(reasearcher, Mockito.times(1)).get("userId");
    }

    @Test
    public void getOwnedSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Mockito.when(coordinator.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("type", SearchFilter.OWNED.name());
        ProjectSpecification ps = ProjectSpecification
                .builder()
                .roles(List.of(Roles.STUDY_COORDINATOR))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .filter(filter)
                .language(Language.en)
                .sortOrder(Sort.Order.asc("status"))
                .build();
        ps.toPredicate(root, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void getArchivedSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Map<String, String> filter = new HashMap<>();
        filter.put("type", SearchFilter.ARCHIVED.name());
        ProjectSpecification ps = ProjectSpecification
                .builder()
                .roles(List.of(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(3L)
                .filter(filter)
                .build();
        ps.toPredicate(root, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
    }

    @Test
    public void getAllExceptArchivedSpecificationTest() {
        Join coordinator = Mockito.mock(Join.class);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(coordinator);
        Path statusPath = Mockito.mock(Path.class);
        Mockito.when(root.get("status")).thenReturn(statusPath);
        Map<String, String> filter = new HashMap<>();
        filter.put("type", SearchFilter.ALL.name());
        ProjectSpecification ps = ProjectSpecification
                .builder()
                .roles(List.of(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(5L)
                .filter(filter)
                .build();
        ps.toPredicate(root, criteriaBuilder);
        Mockito.verify(root, Mockito.times(2)).get("status");
        Mockito.verify(criteriaBuilder, Mockito.times(1)).notEqual(Mockito.eq(statusPath), Mockito.eq(ProjectStatus.ARCHIVED));
    }
}
