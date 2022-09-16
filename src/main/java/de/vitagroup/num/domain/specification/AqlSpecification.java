package de.vitagroup.num.domain.specification;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlCategory;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.AqlSearchFilter;
import de.vitagroup.num.domain.dto.SearchCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.*;
import java.util.*;

@Builder
@Getter
@AllArgsConstructor
public class AqlSpecification implements Specification<Aql> {

    private Map<String, ?> filter;

    @Nonnull
    private String loggedInUserId;

    @Nonnull
    private Long loggedInUserOrganizationId;

    private Set<String> ownersUUID;

    @Nonnull
    private String language;

    @Override
    public Predicate toPredicate(Root<Aql> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        query.groupBy(root.get("id"));

        Join<Aql, UserDetails> owner = root.join("owner", JoinType.INNER);
        Join<UserDetails, Organization> ownerOrganization = owner.join("organization", JoinType.INNER);
        Join<Aql, AqlCategory> aqlCategory = root.join("category", JoinType.LEFT);
        Predicate ownedPred = criteriaBuilder.equal(owner.get("userId"), loggedInUserId);
        Predicate publicAql = criteriaBuilder.equal(root.get("publicAql"), Boolean.TRUE);
        Predicate ownedOrPublic = criteriaBuilder.or(ownedPred, publicAql);
        if (Objects.nonNull(filter)) {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> nameLikePredicates = new ArrayList<>();
            if (StringUtils.isNotEmpty((String) filter.get(SearchCriteria.FILTER_SEARCH_BY_KEY))) {
                String searchInput = "%" + ((String) filter.get(SearchCriteria.FILTER_SEARCH_BY_KEY)).toUpperCase() + "%";
                Predicate alqNameLike = criteriaBuilder.like(criteriaBuilder.upper(root.get("name")), searchInput);
                nameLikePredicates.add(alqNameLike);
                Predicate aqlCategoryNameLike = criteriaBuilder.like(criteriaBuilder.upper(
                        criteriaBuilder.function("json_extract_path_text", String.class, aqlCategory.get("name"),
                                criteriaBuilder.literal(language))
                ), searchInput);
                nameLikePredicates.add(aqlCategoryNameLike);
                if (CollectionUtils.isNotEmpty(ownersUUID)) {
                    Predicate ownerNameLike = owner.get("userId").in(ownersUUID);
                    nameLikePredicates.add(ownerNameLike);
                }
            }
            AqlSearchFilter filterType = filter.containsKey(SearchCriteria.FILTER_BY_TYPE_KEY) ?
                    AqlSearchFilter.valueOf((String) filter.get(SearchCriteria.FILTER_BY_TYPE_KEY)) :
                    AqlSearchFilter.ALL;
            switch (filterType) {
                case ALL: {
                    predicates.add(ownedOrPublic);
                    break;
                }
                case OWNED:
                    predicates.add(ownedPred);
                    break;
                case ORGANIZATION: {
                    Predicate sameOrganization = criteriaBuilder.equal(ownerOrganization.get("id"), loggedInUserOrganizationId);
                    predicates.add(sameOrganization);
                    predicates.add(ownedOrPublic);
                    break;
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
