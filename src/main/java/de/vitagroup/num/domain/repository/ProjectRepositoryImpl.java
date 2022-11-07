package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.specification.ProjectSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

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


        selectProjectQuery.select(root)
                .where(selectProjectPredicate)
                .groupBy(root.get("id"));
        if (pageable.getSort().isSorted()) {
            selectProjectQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }
        List<Project> result = entityManager.createQuery(selectProjectQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(result, pageable, count);
    }
}
