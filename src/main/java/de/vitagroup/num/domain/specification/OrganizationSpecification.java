package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Organization;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrganizationSpecification implements Specification<Organization> {

    private static final String COLUMN_ORGANIZATION_NAME = "name";
    private static final String WILDCARD_PERCENTAGE_SIGN = "%";
    private Map<String, ?> filter;

    @Override
    public Predicate toPredicate(Root<Organization> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if(Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, ?> entry : filter.entrySet()) {
                if (COLUMN_ORGANIZATION_NAME.equals(entry.getKey())) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.upper(root.get(entry.getKey())),
                            WILDCARD_PERCENTAGE_SIGN + ((String) entry.getValue()).toUpperCase() + WILDCARD_PERCENTAGE_SIGN));
                }
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }
        return null;
    }
}
