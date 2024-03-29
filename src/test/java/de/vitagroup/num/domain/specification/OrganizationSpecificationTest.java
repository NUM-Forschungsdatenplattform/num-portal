package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.model.Organization;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.persistence.criteria.*;
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
