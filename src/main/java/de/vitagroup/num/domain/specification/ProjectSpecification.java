package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.UserDetails;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.*;
import java.util.*;

@AllArgsConstructor
public class ProjectSpecification implements Specification<Project> {

    private static final String COLUMN_PROJECT_ID = "id";

    private static final String COLUMN_PROJECT_NAME = "name";

    private static final String ARCHIVED_FILTER = "archived";

    private static final String MY_ORGANIZATION_FILTER = "organizationId";
    private static final String MY_PROJECTS_FILTER = "coordinatorId";
    private static final String WILDCARD_PERCENTAGE_SIGN = "%";

    private Map<String, ?> filter;

    private List<String> roles;

    private String loggedInUserId;

    @Override
    public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> roleBasedPredicates = new ArrayList<>();
        query.groupBy(root.get(COLUMN_PROJECT_ID));
        if (roles.contains(Roles.STUDY_COORDINATOR)) {
            Predicate coordinatorPredicate = criteriaBuilder.equal(root.get("coordinator").get("userId"), loggedInUserId);
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
        Predicate filterPredicate = null;
        if (Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, ?> entry : filter.entrySet()) {
                if (COLUMN_PROJECT_NAME.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry.getValue())) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get(entry.getKey())),
                            WILDCARD_PERCENTAGE_SIGN + ((String) entry.getValue()).toLowerCase() + WILDCARD_PERCENTAGE_SIGN));
                }
                if (ARCHIVED_FILTER.equals(entry.getKey())) {
                    if (Boolean.TRUE.equals(entry.getKey())) {
                        predicates.add(searchByStatus(root, Arrays.asList(ProjectStatus.ARCHIVED)));
                    }
                }
                if (MY_PROJECTS_FILTER.equals(entry.getKey())) {
                    predicates.add(criteriaBuilder.equal(root.get("coordinator").get("userId"), entry.getValue()));
                }

                // maybe add same condition as in FE to exclud archived projects
                if (MY_ORGANIZATION_FILTER.equals(entry.getKey())) {
                    Join<Project, UserDetails> coordinator = root.join("coordinator");
                    Join<UserDetails, Organization> organizationJoin = coordinator.join("organization");
                    predicates.add(criteriaBuilder.equal(organizationJoin.get("id"), entry.getValue()));
                }
            }
            filterPredicate = criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }
        if (Objects.nonNull(filterPredicate)) {
            return criteriaBuilder.and(finalRoleBasedPredicate, filterPredicate);
        } else {
            return finalRoleBasedPredicate;
        }
    }

    private Predicate searchByStatus(Root<Project> root,
                                     @Nonnull List<ProjectStatus> statuses) {
        if (Objects.isNull(statuses)) {
            throw new IllegalArgumentException("status cannot be null");
        }
        return root.get("status").in(statuses);

    }
}
