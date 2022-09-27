package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.SearchFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.criteria.*;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AqlSpecificationTest {

    @Mock
    private Root<Aql> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Test
    public void ownedOrPublicAqlSpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Path publicAql = Mockito.mock(Path.class);
        Mockito.when(root.get("publicAql")).thenReturn(publicAql);
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language("en")
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(1)).get("userId");
    }

    @Test
    public void searchByNameSpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Path publicAql = Mockito.mock(Path.class);
        Mockito.when(root.get("publicAql")).thenReturn(publicAql);
        Join aqlCategory = Mockito.mock(Join.class);
        Mockito.when(root.join("category", JoinType.LEFT)).thenReturn(aqlCategory);
        Map<String, String> filter = new HashMap<>();
        filter.put("search", "some search input");
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language("en")
                .filter(filter)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(1)).get("userId");
        Mockito.verify(root, Mockito.times(1)).get("name");
        Mockito.verify(aqlCategory, Mockito.times(1)).get("name");
    }

    @Test
    public void sameOrganizationSpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Path publicAql = Mockito.mock(Path.class);
        Mockito.when(root.get("publicAql")).thenReturn(publicAql);
        Map<String, String> filter = new HashMap<>();
        filter.put("type", SearchFilter.ORGANIZATION.name());
        Join organization = Mockito.mock(Join.class);
        Mockito.when(owner.join("organization", JoinType.INNER)).thenReturn(organization);

        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language("en")
                .filter(filter)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(1)).get("userId");
        Mockito.verify(root, Mockito.times(0)).get("name");
        Mockito.verify(organization, Mockito.times(1)).get("id");
    }
}
