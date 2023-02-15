package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlCategory;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.Language;
import de.vitagroup.num.domain.dto.SearchFilter;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SlimAqlDto;
import de.vitagroup.num.domain.repository.AqlCategoryRepository;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.specification.AqlSpecification;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.PrivacyException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.parser.AqlParseException;
import org.ehrbase.aqleditor.dto.aql.QueryValidationResponse;
import org.ehrbase.aqleditor.dto.aql.Result;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.*;

@Slf4j
@Service
@AllArgsConstructor
public class AqlService {

  private static final String AUTHOR_NAME = "author";
  private static final String ORGANIZATION_NAME = "organization";

  private static final String AQL_NAME_GERMAN = "name";
  private static final String AQL_NAME_ENGLISH = "nameTranslated";
  private static final String AQL_CREATE_DATE = "createDate";

  private static final String AQL_CATEGORY = "category";
  private static final List<String> AQL_CATEGORY_SORT_FIELDS = Arrays.asList("name-de", "name-en");
  private static final List<String> AQL_QUERY_SORT_FIELDS = Arrays.asList(AQL_NAME_GERMAN, AQL_NAME_ENGLISH, AUTHOR_NAME, ORGANIZATION_NAME, AQL_CREATE_DATE, AQL_CATEGORY);
  private final AqlRepository aqlRepository;
  private final AqlCategoryRepository aqlCategoryRepository;
  private final EhrBaseService ehrBaseService;
  private final ObjectMapper mapper;
  private final UserDetailsService userDetailsService;
  private final PrivacyProperties privacyProperties;
  private final AqlEditorAqlService aqlEditorAqlService;

  private final UserService userService;

  /**
   * Counts the number of aql queries existing in the platform
   *
   * @return The number of existing AQL queries in the platform
   */
  public long countAqls() {
    return aqlRepository.count();
  }

  public Aql getAqlById(Long id, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    var aql =
        aqlRepository.findById(id).orElseThrow(
                () -> new ResourceNotFound(AqlService.class, AQL_NOT_FOUND, String.format(AQL_NOT_FOUND, id)));

    if (aql.isViewable(loggedInUserId)) {
      return aql;
    } else {
      throw new ForbiddenException(AqlService.class, CANNOT_ACCESS_THIS_AQL);
    }
  }
  public Page<Aql> getVisibleAqls(String loggedInUserId, Pageable pageable, SearchCriteria searchCriteria) {
    UserDetails userDetails = userDetailsService.checkIsUserApproved(loggedInUserId);

    Sort sort = validateAndGetSortForAQLQuery(searchCriteria);
    Pageable pageRequest;
    Page<Aql> aqlPage;
    List<Aql> aqlQueries;
    if (!searchCriteria.isSortByAuthor()) {
      if(AQL_CATEGORY.equals(searchCriteria.getSortBy())) {
        // sort send on page request messes up the generated query for order by and ignores what is inside aql specification
        pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
      } else {
        pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
      }
    } else {
      long count = aqlRepository.count();
      // load all aql criterias because sort by author is done in memory
      pageRequest = PageRequest.of(0, count != 0 ? (int) count : 1);
    }
    Set<String> usersUUID = null;
    if (searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY)) {
      String searchValue = (String) searchCriteria.getFilter().get(SearchCriteria.FILTER_SEARCH_BY_KEY);
      usersUUID = userService.findUsersUUID(searchValue);
    }
    Language language = Objects.nonNull(searchCriteria.getLanguage()) ? searchCriteria.getLanguage() : Language.de;
    AqlSpecification aqlSpecification = AqlSpecification.builder()
            .filter(searchCriteria.getFilter())
            .loggedInUserId(loggedInUserId)
            .loggedInUserOrganizationId(userDetails.getOrganization().getId())
            .ownersUUID(usersUUID)
            .language(language)
            .sortOrder(sort.getOrderFor(searchCriteria.getSortBy()))
            .build();
    aqlPage = aqlRepository.findAll(aqlSpecification, pageRequest);
    aqlQueries = new ArrayList<>(aqlPage.getContent());

    if (searchCriteria.isSortByAuthor()) {
      sortAqlQueries(aqlQueries, sort);
      aqlQueries = aqlQueries.stream()
              .skip((long) pageable.getPageNumber() * pageable.getPageSize())
              .limit(pageable.getPageSize())
              .collect(Collectors.toList());
    }
    return new PageImpl<>(aqlQueries, pageable, aqlPage.getTotalElements());
  }

  private void sortAqlQueries(List<Aql> aqlQueries, Sort sort) {
    if (sort != null) {
      Sort.Order authorOrder = sort.getOrderFor(AUTHOR_NAME);
      if (authorOrder != null) {
        Comparator<Aql> byAuthorName = Comparator.comparing(aql -> {
          User owner = userService.getOwner(aql.getOwner().getUserId());
          return owner.getFullName();
        });
        Sort.Direction sortOrder = authorOrder.getDirection();
        if (sortOrder.isAscending()) {
          Collections.sort(aqlQueries, Comparator.nullsLast(byAuthorName));
        } else {
          Collections.sort(aqlQueries, Comparator.nullsLast(byAuthorName.reversed()));
        }
      }
    }
  }
  public Aql createAql(Aql aql, String loggedInUserId, Long aqlCategoryId) {
    var userDetails = userDetailsService.checkIsUserApproved(loggedInUserId);

    if (Objects.nonNull(aqlCategoryId)) {
      AqlCategory aqlCategory = aqlCategoryRepository.findById(aqlCategoryId).orElseThrow(() ->
              new ResourceNotFound(AqlService.class, CATEGORY_BY_ID_NOT_FOUND,
                      String.format(CATEGORY_BY_ID_NOT_FOUND, aqlCategoryId)));
      aql.setCategory(aqlCategory);
    }

    aql.setOwner(userDetails);
    aql.setCreateDate(OffsetDateTime.now());
    aql.setModifiedDate(OffsetDateTime.now());

    return aqlRepository.save(aql);
  }

  public Aql updateAql(Aql aql, Long aqlId, String loggedInUserId, Long aqlCategoryId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    var aqlToEdit =
        aqlRepository
            .findById(aqlId)
            .orElseThrow(() -> new ResourceNotFound(AqlService.class, CANNOT_FIND_AQL, String.format(CANNOT_FIND_AQL, aqlId)));
    if (Objects.nonNull(aqlCategoryId)) {
      AqlCategory aqlCategory = aqlCategoryRepository.findById(aqlCategoryId).orElseThrow(() ->
              new ResourceNotFound(AqlService.class, CATEGORY_BY_ID_NOT_FOUND,
                      String.format(CATEGORY_BY_ID_NOT_FOUND, aqlCategoryId)));
      aqlToEdit.setCategory(aqlCategory);
    }

    if (aqlToEdit.hasEmptyOrDifferentOwner(loggedInUserId)) {
      throw new ForbiddenException( AqlService.class, AQL_EDIT_FOR_AQL_WITH_ID_IS_NOT_ALLOWED_AQL_HAS_DIFFERENT_OWNER,
          String.format(AQL_EDIT_FOR_AQL_WITH_ID_IS_NOT_ALLOWED_AQL_HAS_DIFFERENT_OWNER, aqlId));
    }

    aqlToEdit.setName(aql.getName());
    aqlToEdit.setNameTranslated(aql.getNameTranslated());
    aqlToEdit.setPurpose(aql.getPurpose());
    aqlToEdit.setPurposeTranslated(aql.getPurposeTranslated());
    aqlToEdit.setUse(aql.getUse());
    aqlToEdit.setUseTranslated(aql.getUseTranslated());
    aqlToEdit.setModifiedDate(OffsetDateTime.now());
    aqlToEdit.setQuery(aql.getQuery());
    aqlToEdit.setPublicAql(aql.isPublicAql());

    return aqlRepository.save(aqlToEdit);
  }

  public void deleteById(Long id, String loggedInUserId, List<String> roles) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    var aql =
        aqlRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound(AqlService.class, CANNOT_FIND_AQL, String.format(CANNOT_FIND_AQL, id)));

    if ((aql.isPublicAql() && (roles.contains(Roles.CRITERIA_EDITOR) || roles.contains(Roles.SUPER_ADMIN)))
        || (!aql.hasEmptyOrDifferentOwner(loggedInUserId) && roles.contains(Roles.CRITERIA_EDITOR))) {
      deleteAql(id);
    } else {
      throw new ForbiddenException(AqlService.class, CANNOT_DELETE_AQL, String.format(CANNOT_DELETE_AQL, id));
    }
  }

  /**
   * Searches among a list of visible AQLs.
   *
   * @param name A string contained in the name of the aqls
   * @param filter Type of the search. Search all owned or public, searched owned only or search
   *     among own organization
   * @param loggedInUserId the user ID of the user sending the search request
   * @return the list of AQLs that match the search filters
   */
  public List<Aql> searchAqls(String name, SearchFilter filter, String loggedInUserId) {

    var userDetails = userDetailsService.checkIsUserApproved(loggedInUserId);

    String searchInput = StringUtils.isNotEmpty(name) ? name.toUpperCase() : name;

    switch (filter) {
      case ALL:
        return aqlRepository.findAllOwnedOrPublicByName(userDetails.getUserId(), searchInput);
      case OWNED:
        return aqlRepository.findAllOwnedByName(userDetails.getUserId(), searchInput);
      case ORGANIZATION:
        return aqlRepository.findAllOrganizationOwnedByName(
            userDetails.getOrganization().getId(), userDetails.getUserId(), searchInput);
      default:
        return List.of();
    }
  }

  public long getAqlSize(SlimAqlDto aql, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    validateQuery(aql.getQuery());

    Set<String> ehrIds;
    try {
      ehrIds =
          ehrBaseService.retrieveEligiblePatientIds(Aql.builder().query(aql.getQuery()).build());
    } catch (AqlParseException e) {
      throw new BadRequestException(AqlParseException.class, e.getLocalizedMessage(), e.getMessage());
    }

    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException(AqlService.class, TOO_FEW_MATCHES_RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }
    return ehrIds.size();
  }

  public Page<AqlCategory> getAqlCategories(Pageable pageable, SearchCriteria searchCriteria) {
    Optional<Sort> sortBy = validateAndGetSort(searchCriteria);
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortBy.get());
    return aqlCategoryRepository.findAllCategories(pageRequest);
  }

  public AqlCategory createAqlCategory(String loggedInUserId, AqlCategory aqlCategory) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return aqlCategoryRepository.save(aqlCategory);
  }

  public AqlCategory updateAqlCategory(String loggedInUserId, AqlCategory aqlCategory, Long categoryId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    if (categoryId == null) {
      throw new BadRequestException(AqlCategory.class, CATEGORY_ID_CANT_BE_NULL);
    }
    if (!aqlCategoryRepository.existsById(categoryId)) {
      throw new ResourceNotFound(AqlService.class, CATEGORY_BY_ID_NOT_FOUND, String.format(CATEGORY_BY_ID_NOT_FOUND, categoryId));
    }
    aqlCategory.setId(categoryId);
    return aqlCategoryRepository.save(aqlCategory);
  }

  @Transactional
  public void deleteCategoryById(String loggedInUserId, Long id) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    if (aqlRepository.findByCategoryId(id).isEmpty()) {
      if (aqlCategoryRepository.existsById(id)) {
        aqlCategoryRepository.deleteById(id);
      } else {
        throw new ResourceNotFound(AqlService.class, CATEGORY_WITH_ID_DOES_NOT_EXIST, String.format(CATEGORY_WITH_ID_DOES_NOT_EXIST, id));
      }
    } else {
      throw new BadRequestException(AqlService.class, THE_CATEGORY_IS_NOT_EMPTY_CANT_DELETE_IT);
    }
  }
  public boolean existsById(Long aqlId) {
    return aqlRepository.existsById(aqlId);
  }

  private void validateQuery(String query) {
    QueryValidationResponse response =
        aqlEditorAqlService.validateAql(Result.builder().q(query).build());
    if (!response.isValid()) {
      try {
        throw new BadRequestException(QueryValidationResponse.class, COULD_NOT_SERIALIZE_AQL_VALIDATION_RESPONSE,
                String.format(COULD_NOT_SERIALIZE_AQL_VALIDATION_RESPONSE, mapper.writeValueAsString(response)));
      } catch (JsonProcessingException e) {
        log.error(COULD_NOT_SERIALIZE_AQL_VALIDATION_RESPONSE, e);
      }
    }
  }

  private void deleteAql(Long id) {
    try {
      aqlRepository.deleteById(id);
    } catch (EmptyResultDataAccessException e) {
      throw new BadRequestException(AqlService.class, INVALID_AQL_ID, String.format("%s: %s", INVALID_AQL_ID, id));
    }
  }

  private Optional<Sort> validateAndGetSort(SearchCriteria searchCriteria) {
    if (searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy())) {
      if (!AQL_CATEGORY_SORT_FIELDS.contains(searchCriteria.getSortBy())) {
        throw new BadRequestException(AqlService.class, String.format("Invalid %s sortBy field for aql", searchCriteria.getSortBy()));
      }
      if ("name-de".equals(searchCriteria.getSortBy())) {
        return Optional.of(JpaSort.unsafe(Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()), "name->>'de'"));
      } else if ("name-en".equals(searchCriteria.getSortBy())) {
        return Optional.of(JpaSort.unsafe(Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()), "name->>'en'"));
      }
    }
    return Optional.of(JpaSort.unsafe(Sort.Direction.ASC, "name->>'de'"));
  }

  private Sort validateAndGetSortForAQLQuery(SearchCriteria searchCriteria) {
    if (searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy())) {
      if (!AQL_QUERY_SORT_FIELDS.contains(searchCriteria.getSortBy())) {
        throw new BadRequestException(AqlService.class, String.format("Invalid %s sortBy field for aql queries", searchCriteria.getSortBy()));
      }
      if (ORGANIZATION_NAME.equals(searchCriteria.getSortBy())) {
        return Sort.by(Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()), "owner.organization.name");
      }
      return Sort.by(Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()), searchCriteria.getSortBy());
    }
    return Sort.by(Sort.Direction.DESC, AQL_CREATE_DATE);
  }
}
