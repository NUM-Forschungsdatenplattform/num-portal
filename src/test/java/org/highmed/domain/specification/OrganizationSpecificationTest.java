package org.highmed.domain.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.domain.model.Organization;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationSpecificationTest {

    @Mock
    private Root<Organization> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Test
    public void searchByOrganizationNameSpecificationTest() {
        Mockito.when(root.get("name")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("search", "some organization name");
        OrganizationSpecification organizationSpecification = OrganizationSpecification.builder()
                .filter(filter)
                .build();
        organizationSpecification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("name");

    }

    @Test
    public void searchByOrganizationActiveSpecificationTest() {
        Mockito.when(root.get("active")).thenReturn(Mockito.mock(Path.class));
        Map<String, String> filter = new HashMap<>();
        filter.put("active", "true");
        OrganizationSpecification organizationSpecification = OrganizationSpecification.builder()
                .filter(filter)
                .build();
        organizationSpecification.toPredicate(root, query, criteriaBuilder);
        Mockito.verify(root, Mockito.times(1)).get("active");

    }
}
