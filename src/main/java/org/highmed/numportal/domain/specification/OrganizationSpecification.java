package org.highmed.numportal.domain.specification;

import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.model.Organization;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrganizationSpecification implements Specification<Organization> {

  private static final String WILDCARD_PERCENTAGE_SIGN = "%";
  private Map<String, ?> filter;

  @Override
  public Predicate toPredicate(Root<Organization> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
    if (Objects.nonNull(filter)) {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY)) {
        String entry = (String) filter.get(SearchCriteria.FILTER_SEARCH_BY_KEY);
        if (StringUtils.isNotEmpty(entry)) {
          predicates.add(criteriaBuilder.like(
              criteriaBuilder.upper(root.get("name")),
              WILDCARD_PERCENTAGE_SIGN + entry.toUpperCase() + WILDCARD_PERCENTAGE_SIGN));
        }
      }
      if (filter.containsKey(SearchCriteria.FILTER_BY_ACTIVE_ORGANIZATION)) {
        String entry = (String) filter.get(SearchCriteria.FILTER_BY_ACTIVE_ORGANIZATION);
        if (StringUtils.isNotEmpty(entry)) {
          predicates.add(criteriaBuilder.equal(
              root.get("active"),
              Boolean.valueOf(entry)));
        }
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }
    return null;
  }
}
