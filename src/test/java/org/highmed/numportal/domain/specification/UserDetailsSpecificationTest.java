package org.highmed.numportal.domain.specification;

import jakarta.persistence.criteria.*;
import org.highmed.numportal.domain.specification.UserDetailsSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.numportal.domain.model.admin.UserDetails;

import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsSpecificationTest {

    @Mock
    private Root<UserDetails> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Test
    public void approvedUsersSpecificationTest() {
        Mockito.when(root.get("approved")).thenReturn(Mockito.mock(Path.class));
        UserDetailsSpecification specification = UserDetailsSpecification.builder()
                .approved(Boolean.TRUE)
                .build();
        specification.toPredicate(root, query, criteriaBuilder);
    }

    @Test
    public void sameOrganizationUserSpecificationTest() {
        Join organization = Mockito.mock(Join.class);
        Mockito.when(root.join("organization", JoinType.INNER)).thenReturn(organization);
        UserDetailsSpecification specification = UserDetailsSpecification.builder()
                .loggedInUserOrganizationId(3L)
                .build();
        specification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(organization, Mockito.times(1)).get("id");
    }

    @Test
    public void selectedUserSpecificationTest() {
        Set<String> usersUUID = new HashSet<>();
        usersUUID.add("user-id-1");
        usersUUID.add("user-id-2");
        UserDetailsSpecification specification = UserDetailsSpecification.builder()
                .usersUUID(usersUUID)
                .build();
        Mockito.when(root.get("userId")).thenReturn(Mockito.mock(Path.class));
        specification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("userId");
    }
}
