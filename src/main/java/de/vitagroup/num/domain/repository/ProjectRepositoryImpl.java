package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.EntityGroup;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Translation;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.specification.ProjectSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ProjectRepositoryImpl implements CustomProjectRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Project> findProjects(ProjectSpecification projectSpecification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Project> selectProjectQuery = cb.createQuery(Project.class);
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Project> countRoot = countQuery.from(selectProjectQuery.getResultType());
        Root<Project> root = selectProjectQuery.from(Project.class);


        Predicate selectProjectPredicate = projectSpecification.toPredicate(root, cb);
        Predicate selectCountProjectPredicate = projectSpecification.toPredicate(countRoot, cb);
        countQuery.select(cb.countDistinct(countRoot.get("id")))
                .where(selectCountProjectPredicate);
        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<Expression<?>> groupByExpressions = new ArrayList<>();
        groupByExpressions.add(root.get("id"));
        if (projectSpecification.getSortOrder() != null) {
            Sort.Order sortOrder = projectSpecification.getSortOrder();
            Expression<?> orderByExpression = null;
            if (ProjectSpecification.COORDINATOR_ORGANIZATION.equals(projectSpecification.getSortOrder().getProperty())) {
                Fetch<Project, UserDetails> coordinator = root.fetch("coordinator");
                Fetch<UserDetails, Organization> organization = coordinator.fetch("organization", JoinType.LEFT);
                Join<Project, UserDetails> coordinatorJoin = (Join<Project, UserDetails>) coordinator;
                Join<UserDetails, Organization> organizationJoin = (Join<UserDetails, Organization>) organization;
                groupByExpressions.add(coordinatorJoin.get("userId"));
                groupByExpressions.add(organizationJoin.get("id"));
                orderByExpression = organizationJoin.get("name");
            } else {
                if (ProjectSpecification.COLUMN_PROJECT_STATUS.equals(projectSpecification.getSortOrder().getProperty())) {
                    Join<Object, Object> translationJoin = root.join("translations", JoinType.LEFT);
                    translationJoin.on(cb.and(
                            cb.equal(translationJoin.get("entityGroup"), EntityGroup.PROJECT_STATUS),
                            cb.equal(translationJoin.get("language"), projectSpecification.getLanguage()),
                            cb.equal(translationJoin.get("property"), root.get("status"))
                    ));
                    groupByExpressions.add(translationJoin.get("value"));
                    orderByExpression = translationJoin.get("value");
                } else if (!"author".equals(sortOrder.getProperty())) {
                    orderByExpression = root.get(sortOrder.getProperty());
                }
            }
            if (Objects.nonNull(orderByExpression)) {
                if (sortOrder.getDirection().isAscending()) {
                    selectProjectQuery.orderBy(cb.asc(orderByExpression));
                } else {
                    selectProjectQuery.orderBy(cb.desc(orderByExpression));
                }
            }
        }
        selectProjectQuery.select(root)
                .where(selectProjectPredicate)
                .groupBy(groupByExpressions);
        List<Project> result = entityManager.createQuery(selectProjectQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(result, pageable, count);
    }
}
