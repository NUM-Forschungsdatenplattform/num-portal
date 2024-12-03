package org.highmed.numportal.domain.specification;

import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.admin.UserDetails;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Builder
@AllArgsConstructor
@Getter
public class UserDetailsSpecification implements Specification<UserDetails> {

  private Long loggedInUserOrganizationId;

  private Set<String> usersUUID;

  private Boolean approved;

  @Override
  public Predicate toPredicate(Root<UserDetails> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(usersUUID)) {
      predicates.add(root.get("userId").in(usersUUID));
    }
    if (approved != null) {
      predicates.add(criteriaBuilder.equal(root.get("approved"), approved));
    }
    if (loggedInUserOrganizationId != null) {
      Join<UserDetails, Organization> userOrganization = root.join("organization", JoinType.INNER);
      predicates.add(criteriaBuilder.equal(userOrganization.get("id"), loggedInUserOrganizationId));
    }
    return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
  }
}
