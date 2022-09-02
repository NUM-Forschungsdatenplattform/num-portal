package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.*;
import java.util.*;

@AllArgsConstructor
public class ProjectSpecification implements Specification<Project> {

    private static final String COLUMN_PROJECT_NAME = "name";

    private static final String FILTER_TYPE = "type";

    private static final String WILDCARD_PERCENTAGE_SIGN = "%";

    private Map<String, ?> filter;

    private List<String> roles;

    private String loggedInUserId;

    private Long loggedInUserOrganizationId;

    @Override
    public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> roleBasedPredicates = new ArrayList<>();
        query.groupBy(root.get("id"));

        //root.fetch("coordinator").fetch("organization");
        Join<Project, UserDetails> coordinator = root.join("coordinator", JoinType.INNER);
        Join<UserDetails, Organization> coordinatorOrganization = coordinator.join("organization", JoinType.INNER);

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
        Predicate filterPredicate = null;
        if (Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, ?> entry : filter.entrySet()) {
                if (COLUMN_PROJECT_NAME.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry.getValue())) {
                    String searchValue = WILDCARD_PERCENTAGE_SIGN + ((String) entry.getValue()).toLowerCase() + WILDCARD_PERCENTAGE_SIGN;
                    Predicate projectNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get(entry.getKey())), searchValue);
                    predicates.add(projectNameLike);
                }
                if(FILTER_TYPE.equals(entry.getKey()) && StringUtils.isNotEmpty((String) entry .getValue())) {
                    ProjectFilterType typeValue = ProjectFilterType.valueOf((String) entry.getValue());
                    switch (typeValue) {
                        case MY_PROJECTS: {
                            predicates.add(criteriaBuilder.equal(coordinator.get("userId"), loggedInUserId));
                            predicates.add(criteriaBuilder.notEqual(root.get("status"), ProjectStatus.ARCHIVED));
                            break;
                        }
                        case MY_ORGANIZATION: {
                            predicates.add(criteriaBuilder.equal(coordinatorOrganization.get("id"), loggedInUserOrganizationId));
                            predicates.add(criteriaBuilder.notEqual(root.get("status"), ProjectStatus.ARCHIVED));
                            break;
                        }
                        case ARCHIVED: {
                            predicates.add(searchByStatus(root, Arrays.asList(ProjectStatus.ARCHIVED)));
                            break;
                        }
                    }
                }
            }
            filterPredicate = criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        } else {
            // IN FE all tag shows all projects based on roles except archived ones
            filterPredicate = criteriaBuilder.notEqual(root.get("status"), ProjectStatus.ARCHIVED);
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
