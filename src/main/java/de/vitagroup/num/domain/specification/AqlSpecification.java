package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.model.Aql;
import de.vitagroup.num.domain.model.AqlCategory;
import de.vitagroup.num.domain.model.Organization;
import de.vitagroup.num.domain.model.admin.UserDetails;
import de.vitagroup.num.domain.dto.Language;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuperBuilder
@Getter
public class AqlSpecification extends BaseSpecification implements Specification<Aql> {

    private static final String AQL_CATEGORY = "category";

    @Override
    public Predicate toPredicate(Root<Aql> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        Join<Aql, UserDetails> owner = root.join("owner", JoinType.INNER);
        Predicate ownedPred = criteriaBuilder.equal(owner.get("userId"), loggedInUserId);
        Predicate publicAql = criteriaBuilder.equal(root.get("publicAql"), Boolean.TRUE);
        Predicate ownedOrPublic = criteriaBuilder.or(ownedPred, publicAql);
        if (sortOrder != null && sortOrder.getProperty().equals(AQL_CATEGORY)) {
            Join<Aql, AqlCategory> aqlCategory = root.join(AQL_CATEGORY, JoinType.LEFT);
            Expression aqlCategoryName = criteriaBuilder.function("json_extract_path_text", String.class, aqlCategory.get("name"),
                    criteriaBuilder.literal(language.name()));
            if (sortOrder.getDirection().isAscending()) {
                query.orderBy(criteriaBuilder.asc(aqlCategoryName));
            } else {
                query.orderBy(criteriaBuilder.desc(aqlCategoryName));
            }
        }

        if (Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> nameLikePredicates = new ArrayList<>();
            if (StringUtils.isNotEmpty((String) filter.get(SearchCriteria.FILTER_SEARCH_BY_KEY))) {
                String searchInput = "%" + ((String) filter.get(SearchCriteria.FILTER_SEARCH_BY_KEY)).toUpperCase() + "%";
                Predicate alqNameLike = Language.de.equals(language) ? criteriaBuilder.like(criteriaBuilder.upper(root.get("name")), searchInput) :
                        criteriaBuilder.like(criteriaBuilder.upper(root.get("nameTranslated")), searchInput);
                nameLikePredicates.add(alqNameLike);

                Join<Aql, AqlCategory> aqlCategory = root.join(AQL_CATEGORY, JoinType.LEFT);
                Predicate aqlCategoryNameLike = criteriaBuilder.like(criteriaBuilder.upper(
                        criteriaBuilder.function("json_extract_path_text", String.class, aqlCategory.get("name"),
                                criteriaBuilder.literal(language.name()))
                ), searchInput);

                nameLikePredicates.add(aqlCategoryNameLike);
                if (CollectionUtils.isNotEmpty(ownersUUID)) {
                    Predicate ownerNameLike = owner.get("userId").in(ownersUUID);
                    nameLikePredicates.add(ownerNameLike);
                }
            }
            SearchFilter filterType = filter.containsKey(SearchCriteria.FILTER_BY_TYPE_KEY) ?
                    SearchFilter.valueOf((String) filter.get(SearchCriteria.FILTER_BY_TYPE_KEY)) :
                    SearchFilter.ALL;
            switch (filterType) {
                case ALL -> predicates.add(ownedOrPublic);
                case OWNED -> predicates.add(ownedPred);
                case ORGANIZATION -> {
                    Join<UserDetails, Organization> ownerOrganization = owner.join("organization", JoinType.INNER);
                    Predicate sameOrganization = criteriaBuilder.equal(ownerOrganization.get("id"), loggedInUserOrganizationId);
                    predicates.add(sameOrganization);
                    predicates.add(ownedOrPublic);
                }
            }
            if (CollectionUtils.isNotEmpty(nameLikePredicates)) {
                Predicate finaleNameLike = criteriaBuilder.or(nameLikePredicates.toArray(Predicate[]::new));
                predicates.add(finaleNameLike);
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        } else {
            //all owned or public
            return ownedOrPublic;
        }
    }
}
