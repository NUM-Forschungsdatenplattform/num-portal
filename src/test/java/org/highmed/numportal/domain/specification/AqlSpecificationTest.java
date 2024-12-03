package org.highmed.numportal.domain.specification;

import jakarta.persistence.criteria.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.highmed.numportal.domain.dto.Language;
import org.highmed.numportal.domain.dto.SearchFilter;
import org.highmed.numportal.domain.model.Aql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                .language(Language.en)
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
        Mockito.when(owner.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("search", "some search input");
        Set<String> usersUUID = new HashSet<>();
        usersUUID.add("user-id-1");
        usersUUID.add("user-id-2");
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language(Language.en)
                .filter(filter)
                .ownersUUID(usersUUID)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(2)).get("userId");
        Mockito.verify(root, Mockito.times(1)).get("nameTranslated");
        Mockito.verify(root, Mockito.never()).get("name");
        Mockito.verify(aqlCategory, Mockito.times(1)).get("name");
    }
    @Test
    public void searchByGermanNameSpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Path publicAql = Mockito.mock(Path.class);
        Mockito.when(root.get("publicAql")).thenReturn(publicAql);
        Join aqlCategory = Mockito.mock(Join.class);
        Mockito.when(root.join("category", JoinType.LEFT)).thenReturn(aqlCategory);
        Mockito.when(owner.get("userId")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("search", "some search input");
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language(Language.de)
                .filter(filter)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("name");
        Mockito.verify(root, Mockito.never()).get("nameTranslated");
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
                .language(Language.en)
                .filter(filter)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(1)).get("userId");
        Mockito.verify(root, Mockito.times(0)).get("name");
        Mockito.verify(organization, Mockito.times(1)).get("id");
    }

    @Test
    public void ownedOrPublicAqlOrderByCategorySpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Path publicAql = Mockito.mock(Path.class);
        Mockito.when(root.get("publicAql")).thenReturn(publicAql);
        Join aqlCategory = Mockito.mock(Join.class);
        Mockito.when(root.join("category", JoinType.LEFT)).thenReturn(aqlCategory);
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language(Language.en)
                .sortOrder(Sort.Order.asc("category"))
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("publicAql");
        Mockito.verify(owner, Mockito.times(1)).get("userId");
        Mockito.verify(aqlCategory, Mockito.times(1)).get("name");
    }

    @Test
    public void ownedAqlOrderByCategoryDescSpecificationTest() {
        Join owner = Mockito.mock(Join.class);
        Mockito.when(root.join("owner", JoinType.INNER)).thenReturn(owner);
        Mockito.when(owner.get("userId")).thenReturn(Mockito.mock(Path.class));
        Join aqlCategory = Mockito.mock(Join.class);
        Mockito.when(root.join("category", JoinType.LEFT)).thenReturn(aqlCategory);
        Map<String, String> filter = new HashMap<>();
        filter.put("type", SearchFilter.OWNED.name());
        AqlSpecification ps = AqlSpecification.builder()
                .loggedInUserId("userId")
                .loggedInUserOrganizationId(2L)
                .language(Language.en)
                .sortOrder(Sort.Order.desc("category"))
                .filter(filter)
                .build();
        ps.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(owner, Mockito.times(1)).get("userId");
        Mockito.verify(aqlCategory, Mockito.times(1)).get("name");
        Mockito.verify(query, Mockito.times(1)).orderBy(criteriaBuilder.desc(Mockito.any()));
    }
}
