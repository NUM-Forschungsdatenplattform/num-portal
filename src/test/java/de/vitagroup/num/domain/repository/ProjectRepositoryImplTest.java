package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.specification.ProjectSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryImplTest {

    @InjectMocks
    private ProjectRepositoryImpl projectRepositoryImpl;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Root<Project> root;

    @Mock
    private Root<Project> countRoot;

    @Test
    public void testFindProjects() {
        CriteriaBuilder criteriaBuilder = Mockito.mock(CriteriaBuilder.class);
        Mockito.when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        CriteriaQuery<Project> selectProjectQuery = Mockito.mock(CriteriaQuery.class);
        Mockito.when(criteriaBuilder.createQuery(Project.class)).thenReturn(selectProjectQuery);
        Mockito.when(selectProjectQuery.from(Project.class)).thenReturn(root);
        CriteriaQuery<Long> countQuery = Mockito.mock(CriteriaQuery.class);
        Mockito.when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        Mockito.when(countQuery.from(selectProjectQuery.getResultType())).thenReturn(countRoot);
        Mockito.when(root.join("coordinator", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Mockito.when(root.get("status")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(countRoot.join("coordinator", JoinType.INNER)).thenReturn(Mockito.mock(Join.class));
        Mockito.when(countRoot.get("status")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(countRoot.get("id")).thenReturn(Mockito.mock(Path.class));
        Mockito.when(criteriaBuilder.or(Mockito.any())).thenReturn(Mockito.mock(Predicate.class));
        Mockito.when(criteriaBuilder.countDistinct(Mockito.any())).thenReturn(Mockito.mock(Expression.class));
        Mockito.when(countQuery.select(Mockito.any())).thenReturn(Mockito.mock(CriteriaQuery.class));
        TypedQuery resultQuery = Mockito.mock(TypedQuery.class);
        Mockito.when(entityManager.createQuery(Mockito.any(CriteriaQuery.class))).thenReturn(resultQuery);
        Mockito.when(root.get("id")).thenReturn(Mockito.mock(Path.class));
        CriteriaQuery select = Mockito.mock(CriteriaQuery.class);
        Mockito.when(selectProjectQuery.select(Mockito.any())).thenReturn(select);
        CriteriaQuery where = Mockito.mock(CriteriaQuery.class);
        Mockito.when(select.where(Mockito.any(Expression.class))).thenReturn(where);
        Mockito.when(where.groupBy(Mockito.any(Expression.class))).thenReturn(Mockito.mock(CriteriaQuery.class));
        Mockito.when(resultQuery.setFirstResult(Mockito.anyInt())).thenReturn(resultQuery);
        Mockito.when(resultQuery.setMaxResults(Mockito.anyInt())).thenReturn(resultQuery);
        Mockito.when(resultQuery.getResultList()).thenReturn(Collections.emptyList());
        Mockito.when(resultQuery.getSingleResult()).thenReturn(0L);
        ProjectSpecification ps = ProjectSpecification.builder()
                .roles(Arrays.asList(Roles.STUDY_APPROVER))
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(9L)
                .build();
        projectRepositoryImpl.findProjects(ps, PageRequest.of(0, 100));
    }
}