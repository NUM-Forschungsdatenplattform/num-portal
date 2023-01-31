package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
public class ProjectSpecification {

    private static final String COLUMN_PROJECT_STATUS = "status";

    private static final String WILDCARD_PERCENTAGE_SIGN = "%";

    private Map<String, ?> filter;

    private List<String> roles;

    private String loggedInUserId;

    private Long loggedInUserOrganizationId;

    private Set<String> ownersUUID;

    private Sort.Order sortOrder;

    public Predicate toPredicate(Root<Project> root, CriteriaBuilder criteriaBuilder) {
        List<Predicate> roleBasedPredicates = new ArrayList<>();
        Join<Project, UserDetails> coordinator = root.join("coordinator", JoinType.INNER);

        if (roles.contains(Roles.STUDY_COORDINATOR)) {
            Predicate coordinatorPredicate = criteriaBuilder.equal(coordinator.get("userId"), loggedInUserId);
            Predicate coordinatorStatuses = searchByStatus(root, ProjectStatus.getAllProjectStatusToViewAsCoordinator());
            Predicate combined = criteriaBuilder.or(coordinatorPredicate, coordinatorStatuses);
            roleBasedPredicates.add(combined);
        }
        if (roles.contains(Roles.RESEARCHER)) {
            Predicate researcherPredicate = criteriaBuilder.equal(root.join("researchers", JoinType.LEFT).get("userId"), loggedInUserId);
            Predicate researcherStatuses = searchByStatus(root, ProjectStatus.getAllProjectStatusToViewAsResearcher());
            Predicate combined = criteriaBuilder.and(researcherPredicate, researcherStatuses);
            roleBasedPredicates.add(combined);
        }
        if (roles.contains(Roles.STUDY_APPROVER)) {
            roleBasedPredicates.add(searchByStatus(root, ProjectStatus.getAllProjectStatusToViewAsApprover()));
        }
        Predicate finalRoleBasedPredicate = criteriaBuilder.or(roleBasedPredicates.toArray(Predicate[]::new));
        Predicate filterPredicate;
        if (Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, ?> entry : filter.entrySet()) {
                if (SearchCriteria.FILTER_BY_TYPE_KEY.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry.getValue())) {
                    SearchFilter typeValue = SearchFilter.valueOf((String) entry.getValue());
                    switch (typeValue) {
                        case OWNED: {
                            predicates.add(criteriaBuilder.equal(coordinator.get("userId"), loggedInUserId));
                            predicates.add(criteriaBuilder.notEqual(root.get(COLUMN_PROJECT_STATUS), ProjectStatus.ARCHIVED));
                            break;
                        }
                        case ORGANIZATION: {
                            Join<UserDetails, Organization> coordinatorOrganization = coordinator.join("organization", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(coordinatorOrganization.get("id"), loggedInUserOrganizationId));
                            predicates.add(criteriaBuilder.notEqual(root.get(COLUMN_PROJECT_STATUS), ProjectStatus.ARCHIVED));
                            break;
                        }
                        case ARCHIVED: {
                            predicates.add(searchByStatus(root, Arrays.asList(ProjectStatus.ARCHIVED)));
                            break;
                        }
                        case ALL: {
                            // IN FE default ALL tag shows all projects based on roles except archived ones
                            predicates.add(criteriaBuilder.notEqual(root.get(COLUMN_PROJECT_STATUS), ProjectStatus.ARCHIVED));
                            break;
                        }
                    }
                }
                if (SearchCriteria.FILTER_SEARCH_BY_KEY.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry.getValue())) {
                    String searchValue = WILDCARD_PERCENTAGE_SIGN + ((String) entry.getValue()).toUpperCase() + WILDCARD_PERCENTAGE_SIGN;
                    Predicate projectNameLike = criteriaBuilder.like(criteriaBuilder.upper(root.get("name")), searchValue);
                    Predicate authorNameLike = null;
                    if (CollectionUtils.isNotEmpty(ownersUUID)) {
                        authorNameLike = coordinator.get("userId").in(ownersUUID);
                    }
                    if (Objects.nonNull(authorNameLike)) {
                        predicates.add(criteriaBuilder.or(projectNameLike, authorNameLike));
                    } else {
                        predicates.add(projectNameLike);
                    }
                }
                if (SearchCriteria.FILTER_BY_STATUS.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry.getValue())) {
                    predicates.add(searchByStatus(root, getStatusFilter((String) entry.getValue())));
                }
            }
            filterPredicate = criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        } else {
            // IN FE all tag shows all projects based on roles except archived ones
            filterPredicate = criteriaBuilder.notEqual(root.get(COLUMN_PROJECT_STATUS), ProjectStatus.ARCHIVED);
        }
        if (Objects.nonNull(filterPredicate)) {
            return criteriaBuilder.and(finalRoleBasedPredicate, filterPredicate);
        } else {
            return finalRoleBasedPredicate;
        }
    }

    private Predicate searchByStatus(Root<Project> root,
                                     List<ProjectStatus> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            throw new IllegalArgumentException("status cannot be null");
        }
        return root.get(COLUMN_PROJECT_STATUS).in(statuses);
    }

    private List<ProjectStatus> getStatusFilter(String status) {
        return Arrays.stream(status.split(","))
                .map(ProjectStatus::valueOf)
                .collect(Collectors.toList());
    }
}
