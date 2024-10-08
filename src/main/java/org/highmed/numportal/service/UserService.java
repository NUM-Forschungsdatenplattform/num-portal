package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.Language;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.dto.SearchFilter;
import org.highmed.numportal.domain.dto.UserNameDto;
import org.highmed.numportal.domain.model.EntityGroup;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.Translation;
import org.highmed.numportal.domain.model.admin.Role;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.TranslationRepository;
import org.highmed.numportal.domain.repository.UserDetailsRepository;
import org.highmed.numportal.domain.specification.UserDetailsSpecification;
import org.highmed.numportal.mapper.OrganizationMapper;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.metric.UsersMetrics;
import org.highmed.numportal.service.notification.NotificationService;
import org.highmed.numportal.service.notification.dto.Notification;
import org.highmed.numportal.service.notification.dto.account.RolesUpdateNotification;
import org.highmed.numportal.service.notification.dto.account.UserNameUpdateNotification;
import org.highmed.numportal.web.feign.KeycloakFeign;

import feign.FeignException;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CACHE_IS_NOT_REACHABLE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_APPROVED_USER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_ENABLED_USER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CAN_ONLY_CHANGE_OWN_NAME_ORG_ADMIN_NAMES_OF_THE_PEOPLE_IN_THE_ORGANIZATION_AND_SUPERUSER_ALL_NAMES;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.FETCHING_USER_FROM_KEYCLOAK_FAILED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NOT_ALLOWED_TO_REMOVE_THAT_ROLE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NOT_ALLOWED_TO_SET_THAT_ROLE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NOT_ALLOWED_TO_UPDATE_OWN_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NO_ROLES_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_ADMIN_CAN_ONLY_MANAGE_USERS_IN_THEIR_OWN_ORGANIZATION;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ROLE_OR_USER_NOT_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.UNKNOWN_ROLE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  public static final String TRANSLATION_CACHE = "translation";
  public static final String USERS_CACHE = "users";
  private static final String KEYCLOACK_DEFAULT_ROLES_PREFIX = "default-roles-";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String ORGANIZATION_NAME = "organization";
  private static final String REGISTRATION_DATE = "registrationDate";
  private static final String MAIL = "email";
  private static final String ACTIVE = "enabled";
  private static final String LOG_KEYCLOAK_DELETE_USER = "Keycloak call to delete user {}";
  private static final String LOG_KEYCLOAK_ERROR_GET_USER = "Keycloak - could not retrieve user {}. Message: {}, status: {}";
  private static final String LOG_KEYCLOAK_ERROR_RESOURCE_NOT_FOUND = "Keycloak - resource not found. Message: {}, status: {}";
  private static final String LOG_KEYCLOAK_ERROR = "Keycloak - error occurred. Message: {}, status: {}";
  private final UserDetailsRepository userDetailsRepository;
  private final KeycloakFeign keycloakFeign;
  private final UserDetailsService userDetailsService;
  private final OrganizationMapper organizationMapper;
  private final NotificationService notificationService;
  private final CacheManager cacheManager;
  private final TranslationRepository translationRepository;
  private final List<String> availableSortFields = Arrays.asList(FIRST_NAME, LAST_NAME, ORGANIZATION_NAME, REGISTRATION_DATE, MAIL);
  private UsersMetrics usersMetrics;

  @Transactional
  public void initializeUsersCache() {
    List<String> usersUUID = userDetailsService.getAllUsersUUID();
    ConcurrentMapCache usersCache = (ConcurrentMapCache) cacheManager.getCache(USERS_CACHE);
    if (usersCache != null) {
      for (String uuid : usersUUID) {
        try {
          User user = getUserById(uuid, true);
          usersCache.put(uuid, user);
        } catch (ResourceNotFound fe) {
          log.warn("skip cache user {} because not found in keycloak", uuid);
        }
      }
    }
  }

  @Transactional
  public void initializeTranslationCache() {
    ConcurrentMapCache translationsCache = (ConcurrentMapCache) cacheManager.getCache(TRANSLATION_CACHE);
    if (translationsCache != null) {
      for (Translation t : translationRepository.findAll()) {
        translationsCache.put(t.getId(), t);
      }
    }
  }

  @Transactional
  @CachePut(value = USERS_CACHE, key = "#uuid")
  public User addUserToCache(String uuid) {
    return getUserById(uuid, true);
  }

  @Transactional
  @CacheEvict(cacheNames = USERS_CACHE, key = "#userId")
  public void deleteUser(String userId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    Optional<UserDetails> userDetails = userDetailsService.getUserDetailsById(userId);

    if (userDetails.isEmpty()) {
      deleteNotVerifiedUser(userId);
    } else {
      if (userDetails.get().isNotApproved()) {
        deleteNotVerifiedUser(userId);
        userDetailsService.deleteUserDetails(userId);
      } else {
        throw new BadRequestException(UserService.class, CANNOT_DELETE_APPROVED_USER,
            String.format(CANNOT_DELETE_APPROVED_USER, userId));
      }
    }
    log.info("Logged in user {} deleted user with id {} ", loggedInUserId, userId);
  }

  @Transactional
  public User getUserProfile(String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserById(loggedInUserId, true);
  }

  @Transactional
  public User getUserById(String userId, boolean withRole, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserById(userId, withRole);
  }

  /**
   * Retrieves user, portal user details and corresponding roles from identity provider
   *
   * @param userId External id of the user
   * @return User
   */
  @Transactional
  public User getUserById(String userId, Boolean withRole) {
    User user = getUser(userId);

    if (BooleanUtils.isTrue(withRole)) {
      addRoles(user);
    }
    addUserDetails(user);
    return user;
  }

  /**
   * Retrieves user details without roles - used for determining the ownership on aql, projects and comments; caches results
   *
   * @param userId the id of the user to fetch
   * @return the found user
   */
  @Transactional
  @Cacheable(value = USERS_CACHE, key = "#userId")
  @Nullable
  public User getOwner(String userId) {
    try {
      return getUserById(userId, false);
    } catch (ResourceNotFound e) {
      return null;
    }
  }

  /**
   * Retrieves a set of user roles for a particular user from identity provider
   *
   * @param userId External id of the user
   * @return Set of roles
   */
  public Set<Role> getUserRoles(String userId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserRoles(userId);
  }

  private Set<Role> getUserRoles(String userId) {
    try {
      return keycloakFeign.getRolesOfUser(userId);
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      log.error("Keycloak - failed to get user's {} roles. Message {}, status {}", userId, e.getMessage(), e.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      log.error(LOG_KEYCLOAK_ERROR_RESOURCE_NOT_FOUND, e.getMessage(), e.status());
      throw new ResourceNotFound(UserService.class, NO_ROLES_FOUND);
    }
  }

  /**
   * Assigns roles to a particular user
   *
   * @param userId    the user to update roles of
   * @param roleNames The list of roles of the user
   */
  public List<String> setUserRoles(String userId, @NotNull List<String> roleNames, String callerUserId, List<String> callerRoles) {
    validateUserRolesAndOrganization(callerUserId, userId, callerRoles);
    Role[] removeRoles;
    Role[] addRoles;
    try {
      Map<String, Role> supportedRoles =
          keycloakFeign.getRoles().stream().collect(Collectors.toMap(Role::getName, role -> role));

      Set<Role> existingRoles = keycloakFeign.getRolesOfUser(userId);

      removeRoles =
          existingRoles.stream()
                       .filter(role -> !roleNames.contains(role.getName()))
                       .toList()
                       .toArray(new Role[]{});

      addRoles =
          roleNames.stream()
                   .filter(
                       role -> existingRoles.stream().noneMatch(role1 -> role1.getName().equals(role)))
                   .map(supportedRoles::get)
                   .peek(
                       role -> {
                         if (role == null) {
                           throw new BadRequestException(UserService.class, UNKNOWN_ROLE);
                         }
                       })
                   .toList()
                   .toArray(new Role[]{});

      if (Arrays.stream(removeRoles)
                .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException(UserService.class, NOT_ALLOWED_TO_REMOVE_THAT_ROLE);
      }
      if (Arrays.stream(addRoles)
                .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException(UserService.class, NOT_ALLOWED_TO_SET_THAT_ROLE);
      }

      if (removeRoles.length > 0) {
        log.debug("Keycloak call to remove user's {} roles", userId);
        keycloakFeign.removeRoles(userId, removeRoles);
      }

      if (addRoles.length > 0) {
        log.debug("Keycloak call to add roles to user {} ", userId);
        keycloakFeign.addRoles(userId, addRoles);
      }

    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      log.error(LOG_KEYCLOAK_ERROR, e.getMessage(), e.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      log.error(LOG_KEYCLOAK_ERROR_RESOURCE_NOT_FOUND, e.getMessage(), e.status());
      throw new ResourceNotFound(UserService.class, ROLE_OR_USER_NOT_FOUND);
    }

    Set<Role> current = getUserRoles(userId);

    notificationService.send(
        collectRolesUpdateNotification(userId, callerUserId, addRoles, removeRoles, current));
    return roleNames;
  }

  private Set<String> getUserRoleNames(String userId) {
    return getUserRoles(userId).stream().map(Role::getName).collect(Collectors.toSet());
  }

  private void addUserDetails(User user) {
    if (user == null) {
      return;
    }

    Optional<UserDetails> userDetails = userDetailsService.getUserDetailsById(user.getId());

    if (userDetails.isPresent()) {
      user.setApproved(userDetails.get().isApproved());

      if (userDetails.get().getOrganization() != null) {
        user.setOrganization(organizationMapper.convertToDto(userDetails.get().getOrganization()));
      }
    }
  }

  private void addRoles(User user) {
    Set<Role> roles = getUserRoles(user.getId());
    user.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
  }

  /**
   * Retrieved a list of users that match the search criteria
   *
   * @param searchCriteria filter[approved]  Indicates that the user has been approved by the admin, filter[search]    A string contained in username,
   *                       first or last name, or email filter[withRoles] flag whether to add roles to the user structure, if present, or not
   * @return the users that match the search parameters and with optional roles if indicated
   */
  @Transactional
  public Page<User> searchUsers(String loggedInUserId, List<String> callerRoles, SearchCriteria searchCriteria,
      Pageable pageable) {
    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);
    validateSort(searchCriteria);

    Set<String> usersUUID = new HashSet<>();
    List<String> requestedRoles = Collections.emptyList();
    boolean searchCriteriaProvided =
        searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY);
    boolean filterByRoles = searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_BY_ROLES);
    if (filterByRoles) {
      requestedRoles = getRequestedRoles(retrieveSearchField(searchCriteria, SearchCriteria.FILTER_BY_ROLES));
    }
    boolean filterByActiveFlag = searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_BY_ACTIVE);
    Optional<Boolean> activeFlag = Optional.empty();
    if (filterByActiveFlag) {
      activeFlag = Optional.of(Boolean.valueOf(retrieveSearchField(searchCriteria, SearchCriteria.FILTER_BY_ACTIVE)));
    }
    if (searchCriteriaProvided || CollectionUtils.isNotEmpty(requestedRoles) || filterByActiveFlag) {
      String searchValue = retrieveSearchField(searchCriteria, SearchCriteria.FILTER_SEARCH_BY_KEY);
      usersUUID = this.filterKeycloakUsers(searchValue, requestedRoles, activeFlag, searchCriteria.getLanguage());
    }
    if (CollectionUtils.isEmpty(usersUUID) && (searchCriteriaProvided || CollectionUtils.isNotEmpty(requestedRoles) || filterByActiveFlag)) {
      return Page.empty(pageable);
    }
    Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    UserDetailsSpecification userDetailsSpecification = buildUserSpecification(loggedInUser, callerRoles, searchCriteria, usersUUID);
    if (isSortActive(searchCriteria)) {
      long count = userDetailsService.countUserDetails();
      pageRequest = PageRequest.of(0, count != 0 ? (int) count : 1);
    }
    Page<UserDetails> userDetailsPage = userDetailsService.getUsers(pageRequest, userDetailsSpecification);
    List<UserDetails> userDetailsList = userDetailsPage.getContent();
    Set<String> filteredUsersUUID = userDetailsList.stream().map(UserDetails::getUserId).collect(Collectors.toSet());
    List<User> filteredUsers = new ArrayList<>();

    Boolean withRoles = searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_USER_WITH_ROLES_KEY)
        ? Boolean.valueOf((String) searchCriteria.getFilter().get(SearchCriteria.FILTER_USER_WITH_ROLES_KEY)) : null;
    boolean loadUserRoles = (withRoles != null && withRoles) || Roles.isProjectLead(callerRoles);
    for (String uuid : filteredUsersUUID) {
      try {
        User user = getUser(uuid, loadUserRoles);
        filteredUsers.add(user);
      } catch (ResourceNotFound rnf) {
        log.warn("For unknown reasons, user with uuid {} was not found in cache, neither in keycloack ", uuid);
      }
    }
    if (isSortActive(searchCriteria)) {
      sortUsers(filteredUsers, searchCriteria);
      filteredUsers = filteredUsers.stream()
                                   .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                                   .limit(pageable.getPageSize())
                                   .collect(Collectors.toList());
    }
    return new PageImpl<>(new ArrayList<>(filteredUsers), pageable, userDetailsPage.getTotalElements());
  }

  private <T> T retrieveSearchField(SearchCriteria searchCriteria, String fieldKey) {
    if (searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(fieldKey)) {
      return (T) searchCriteria.getFilter().get(fieldKey);
    }
    return null;
  }

  private UserDetailsSpecification buildUserSpecification(UserDetails loggedInUser, List<String> callerRoles, SearchCriteria searchCriteria,
      Set<String> usersUUID) {
    Boolean approved = null;
    Long organizationId = null;

    if (searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_APPROVED_KEY)) {
      approved = Boolean.valueOf((String) searchCriteria.getFilter().get(SearchCriteria.FILTER_APPROVED_KEY));
    }
    if (!Roles.isSuperAdmin(callerRoles) && Roles.isOrganizationAdmin(callerRoles) && loggedInUser.getOrganization() != null) {
      // super admin receives all users
      // organization admin should receive users only for his organization
      organizationId = loggedInUser.getOrganization().getId();
    }
    if (!Roles.isSuperAdmin(callerRoles) && Roles.isProjectLead(callerRoles)) {
      // project lead/study_coordinator should see only researchers
      usersUUID.removeIf(uuid -> !getUserRoleNames(uuid).contains(Roles.RESEARCHER));
    }
    if (searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_BY_TYPE_KEY)) {
      if (SearchFilter.ORGANIZATION.equals(SearchFilter.valueOf((String) searchCriteria.getFilter().get(SearchCriteria.FILTER_BY_TYPE_KEY)))) {
        organizationId = loggedInUser.getOrganization().getId();
      }
    }
    return UserDetailsSpecification.builder()
                                   .approved(approved)
                                   .loggedInUserOrganizationId(organizationId)
                                   .usersUUID(usersUUID)
                                   .build();
  }

  private void sortUsers(List<User> users, SearchCriteria searchCriteria) {
    String field = searchCriteria.getSortBy() != null ? searchCriteria.getSortBy() : REGISTRATION_DATE;
    Sort.Direction sortOrder =
        searchCriteria.getSort() != null ? Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()) : Sort.Direction.DESC;
    Comparator<User> userComparator = getComparator(field);
    if (sortOrder.isAscending()) {
      users.sort(Comparator.nullsLast(userComparator));
    } else {
      users.sort(Comparator.nullsLast(userComparator.reversed()));
    }
  }

  private Comparator<User> getComparator(String field) {
    return switch (field) {
      case FIRST_NAME -> Comparator.comparing(u -> u.getFirstName().toUpperCase());
      case LAST_NAME -> Comparator.comparing(u -> u.getLastName().toUpperCase());
      case ORGANIZATION_NAME ->
          Comparator.comparing(user -> user.getOrganization() != null ? user.getOrganization().getName().toUpperCase() : StringUtils.EMPTY);
      case MAIL -> Comparator.comparing(u -> u.getEmail().toUpperCase());
      default -> Comparator.comparing(User::getCreatedTimestamp);
    };
  }

  @Transactional
  public Set<User> getByRole(String role) {
    try {
      Set<User> users = keycloakFeign.getByRole(role);
      users.removeIf(u -> userDetailsService.getUserDetailsById(u.getId()).isEmpty());
      users.forEach(this::addUserDetails);
      return users;
    } catch (FeignException fe) {
      log.error(LOG_KEYCLOAK_ERROR, fe.getMessage(), fe.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER, fe.getMessage()));
    }
  }

  private List<Notification> collectUserNameUpdateNotification(
      String userId, String loggedInUserId) {
    List<Notification> notifications = new LinkedList<>();
    User user = getUserById(userId, false);
    User admin = getUserById(loggedInUserId, false);

    if (user != null && admin != null) {
      UserNameUpdateNotification not =
          UserNameUpdateNotification.builder()
                                    .recipientEmail(user.getEmail())
                                    .recipientFirstName(user.getFirstName())
                                    .recipientLastName(user.getLastName())
                                    .adminEmail(admin.getEmail())
                                    .adminFullName(String.format("%s %s", admin.getFirstName(), admin.getLastName()))
                                    .build();

      notifications.add(not);
    } else {
      log.warn("Could not create profile update email notification.");
    }

    return notifications;
  }

  private List<Notification> collectRolesUpdateNotification(
      String userId,
      String loggedInUserId,
      Role[] rolesAdded,
      Role[] rolesRemoved,
      Set<Role> allRoles) {

    List<Notification> notifications = new LinkedList<>();
    User user = getUserById(userId, false);
    User admin = getUserById(loggedInUserId, false);
    allRoles.removeIf(r -> r.getName().startsWith(KEYCLOACK_DEFAULT_ROLES_PREFIX));

    if (user != null && admin != null && (rolesAdded.length > 0 || rolesRemoved.length > 0)) {
      RolesUpdateNotification notification =
          RolesUpdateNotification.builder()
                                 .recipientEmail(user.getEmail())
                                 .recipientFirstName(user.getFirstName())
                                 .recipientLastName(user.getLastName())
                                 .adminEmail(admin.getEmail())
                                 .adminFullName(String.format("%s %s", admin.getFirstName(), admin.getLastName()))
                                 .rolesRemoved(
                                     Arrays.stream(rolesRemoved).map(Role::getName).collect(Collectors.toList()))
                                 .rolesAdded(Arrays.stream(rolesAdded).map(Role::getName).collect(Collectors.toList()))
                                 .allRoles(allRoles.stream().map(Role::getName).collect(Collectors.toList()))
                                 .build();

      notifications.add(notification);
    }

    return notifications;
  }

  private void deleteNotVerifiedUser(String userId) {
    User user = keycloakFeign.getUser(userId);
    if (user != null && BooleanUtils.isFalse(user.getEmailVerified())) {
      log.debug(LOG_KEYCLOAK_DELETE_USER, userId);
      try {
        keycloakFeign.deleteUser(userId);
      } catch (FeignException e) {
        log.error(LOG_KEYCLOAK_ERROR, e.getMessage(), e.status());
        throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER,
            String.format(AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
      }
    } else {
      throw new BadRequestException(UserService.class, CANNOT_DELETE_ENABLED_USER);
    }
  }

  /**
   * Refresh users cache every 8 hours
   */
  @Scheduled(fixedRate = 28800000)
  @Transactional
  public void refreshUsersCache() {
    var cache = cacheManager.getCache(USERS_CACHE);
    if (cache != null) {
      log.trace("---- Refreshing users cache ----");
      cache.clear();
      initializeUsersCache();
    }
  }

  private void deleteNotApprovedUser(UserDetails userDetails) {
    String userId = userDetails.getUserId();
    try {
      if (userId != null) {

        Timestamp createdAt;
        if (isNull(userDetails.getCreatedDate())) {
          User userForDeletion = keycloakFeign.getUser(userDetails.getUserId());
          if (isNull(userForDeletion.getCreatedTimestamp())) {
            return;
          } else {
            createdAt = new Timestamp(userForDeletion.getCreatedTimestamp());
          }
        } else {
          createdAt = new Timestamp(userDetails.getCreatedDate().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        Date createdAtDate = new Date(createdAt.getTime());
        boolean shouldDelete = LocalDateTime.from(createdAtDate.toInstant().atZone(ZoneId.of("UTC")))
                                            .plusDays(30).isBefore(LocalDateTime.now());
        if (shouldDelete) {
          log.debug(LOG_KEYCLOAK_DELETE_USER, userId);
          keycloakFeign.deleteUser(userId);
          userDetailsService.deleteUserDetails(userId);
          log.info("- deleteUnapprovedUsersAfter30Days - userID: {} isApproved: {} deletedUser: {}", userId, userDetails.isApproved(), userDetails);
        }
      }
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      log.error(LOG_KEYCLOAK_ERROR, e.getMessage(), e.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      log.error(LOG_KEYCLOAK_ERROR_GET_USER, userId, e.getMessage(), e.status());
      throw new ResourceNotFound(UserService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, userId));
    }
  }

  @Scheduled(cron = "${user-service.delete-users-cron}", zone = "UTC")//0 0 5 * * *
  @Transactional
  public void deleteUnapprovedUsersAfter30Days() {
    List<UserDetails> users = userDetailsRepository.findAllByApproved(false).orElse(new ArrayList<>());
    users.forEach(this::deleteNotApprovedUser);
  }

  @CachePut(cacheNames = USERS_CACHE, key = "#userIdToChange")
  @Transactional
  public User changeUserName(
      @NotNull String userIdToChange,
      @NotNull UserNameDto userName,
      @NotNull String loggedInUserId,
      List<String> roles) {

    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);
    UserDetails userToChange = userDetailsService.checkIsUserApproved(userIdToChange);

    if (Roles.isSuperAdmin(roles)
        || isSelf(loggedInUser, userToChange)
        || (Roles.isOrganizationAdmin(roles)
        && belongToSameOrganization(loggedInUser, userToChange))) {
      updateName(userIdToChange, userName);
      log.info("User's {} name was changed by {}", userIdToChange, loggedInUserId);
    } else {
      throw new ForbiddenException(
          UserService.class,
          CAN_ONLY_CHANGE_OWN_NAME_ORG_ADMIN_NAMES_OF_THE_PEOPLE_IN_THE_ORGANIZATION_AND_SUPERUSER_ALL_NAMES);
    }

    notificationService.send(collectUserNameUpdateNotification(userIdToChange, loggedInUserId));
    return getUserById(userIdToChange, true);
  }

  /**
   * update active Field of user
   *
   * @param loggedInUserId
   * @param userId
   * @param active
   * @param callerRoles
   */
  @CachePut(cacheNames = USERS_CACHE, key = "#userId")
  @Transactional
  public User updateUserActiveField(@NotNull String loggedInUserId, @NotNull String userId, @NotNull Boolean active, List<String> callerRoles) {
    validateUserRolesAndOrganization(loggedInUserId, userId, callerRoles);
    if (Objects.equals(loggedInUserId, userId)) {
      throw new ForbiddenException(UserService.class, NOT_ALLOWED_TO_UPDATE_OWN_STATUS);
    }
    return updateUserActiveField(loggedInUserId, userId, active);
  }

  @CachePut(cacheNames = USERS_CACHE, key = "#userId")
  @Transactional
  public User updateUserActiveField(@NotNull String loggedInUserId, @NotNull String userId, @NotNull Boolean active) {
    Map<String, Object> userRaw = getUserRaw(userId);
    if (!active.equals(userRaw.get(ACTIVE))) {
      // call keycloak rest API only if status changed
      log.info("User {} flag active was changed to {} by loggedInUser {} ", userId, active, loggedInUserId);
      userRaw.put(ACTIVE, active);
      log.debug("Keycloak call to update user's {} 'enabled' field", userId);
      keycloakFeign.updateUser(userId, userRaw);
      userDetailsService.sendAccountStatusChangedNotification(userId, loggedInUserId, active);
      usersMetrics.updateCountStatus(active);
    }
    return getUserById(userId, true);
  }

  private void validateUserRolesAndOrganization(String loggedInUserId, String userId, List<String> callerRoles) {
    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);
    UserDetails userToUpdate = userDetailsService.getUserDetailsById(userId).orElseThrow(() ->
        new SystemException(UserService.class, USER_NOT_FOUND,
            String.format(USER_NOT_FOUND, userId)));
    if (Roles.isOrganizationAdmin(callerRoles)
        && !Roles.isSuperAdmin(callerRoles)
        && !belongToSameOrganization(loggedInUser, userToUpdate)) {
      throw new ForbiddenException(UserService.class, ORGANIZATION_ADMIN_CAN_ONLY_MANAGE_USERS_IN_THEIR_OWN_ORGANIZATION);
    }
  }

  /**
   * Retrieved a list of users UUID that match the search criteria
   *
   * @param search A string contained in username, first or last name, or email
   * @return a Set of filteredUsers
   */
  public Set<String> findUsersUUID(String search) {
    return filterKeycloakUsers(search, Collections.emptyList(), Optional.empty(), Language.en);
  }

  private Set<String> filterKeycloakUsers(String search, List<String> roles, Optional<Boolean> enabledFlag, Language language) {
    Set<String> userUuids = new HashSet<>();
    ConcurrentMapCache usersCache = (ConcurrentMapCache) cacheManager.getCache(USERS_CACHE);
    if ((StringUtils.isNotEmpty(search) || CollectionUtils.isNotEmpty(roles) || enabledFlag.isPresent())
        && usersCache != null && usersCache.getNativeCache().size() != 0) {
      ConcurrentMap<Object, Object> users = usersCache.getNativeCache();
      for (Map.Entry<Object, Object> entry : users.entrySet()) {
        filterUsers(search, roles, enabledFlag, userUuids, entry, language);
      }
      return userUuids;
    }
    return Collections.emptySet();
  }

  private void filterUsers(String search, List<String> roles, Optional<Boolean> enabledFlag,
      Set<String> userUuids, Map.Entry<Object, Object> entry, Language language) {
    boolean filterByNameEnabled = StringUtils.isNotEmpty(search);
    boolean filterByRoleEnabled = CollectionUtils.isNotEmpty(roles);
    Set<Translation> translations = getTranslated(EntityGroup.ROLE_NAME, language);
    List<String> rolesTranslated =
        search != null
            ? translations.stream().filter(t -> t.getValue().toUpperCase().contains(search.toUpperCase())).map(Translation::getProperty).toList()
            : new ArrayList<>();

    if (entry.getValue() instanceof User user) {
      boolean enabledFilter = enabledFlag.isEmpty() || enabledFlag.get().equals(user.getEnabled());
      if (filterByNameEnabled && filterByRoleEnabled) {
        if ((StringUtils.containsIgnoreCase(user.getFullName(), search) || StringUtils.containsIgnoreCase(user.getEmail(), search)
            || CollectionUtils.containsAny(user.getRoles(), rolesTranslated))
            && nonNull(user.getRoles()) && CollectionUtils.containsAny(user.getRoles(), roles) && enabledFilter) {
          userUuids.add((String) entry.getKey());
        }
      } else if (filterByNameEnabled && (StringUtils.containsIgnoreCase(user.getFullName(), search)
          || StringUtils.containsIgnoreCase(user.getEmail(), search) || nonNull(user.getRoles()) && CollectionUtils.containsAny(
          user.getRoles(), rolesTranslated))
          && enabledFilter) {
        userUuids.add((String) entry.getKey());
      } else if (filterByRoleEnabled && CollectionUtils.containsAny(user.getRoles(), roles) && enabledFilter) {
        userUuids.add((String) entry.getKey());
      } else if (!filterByNameEnabled && !filterByRoleEnabled && enabledFlag.isPresent() && enabledFilter) {
        userUuids.add((String) entry.getKey());
      }
    }
  }

  private Set<Translation> getTranslated(EntityGroup entityGroup, Language language) {
    ConcurrentMap<Long, Translation> cm = cacheManager.getCache(TRANSLATION_CACHE) != null
        ? (ConcurrentMap<Long, Translation>) Objects.requireNonNull(cacheManager.getCache(TRANSLATION_CACHE)).getNativeCache()
        : null;
    if (isNull(cm)) {
      throw new ResourceNotFound(UserService.class, CACHE_IS_NOT_REACHABLE);
    }
    Set<Translation> translationList = new HashSet<>();
    cm.forEach((aLong, translation) -> {
      if (nonNull(language) && translation.getLanguage().compareTo(language) == 0 && translation.getEntityGroup() == entityGroup) {
        translationList.add(translation);
      }
    });
    return translationList;
  }

  private void updateName(String userId, UserNameDto userNameDto) {
    Map<String, Object> userRaw = getUserRaw(userId);
    userRaw.put(FIRST_NAME, userNameDto.getFirstName());
    userRaw.put(LAST_NAME, userNameDto.getLastName());
    log.debug("Keycloak call to update user's name {} ", userId);
    try {
      keycloakFeign.updateUser(userId, userRaw);
    } catch (FeignException fe) {
      log.error(LOG_KEYCLOAK_ERROR, fe.getMessage(), fe.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER, fe.getMessage()));
    }

  }

  private boolean belongToSameOrganization(UserDetails user1, UserDetails user2) {
    return user1 != null
        && user2 != null
        && user1.getOrganization() != null
        && user2.getOrganization() != null
        && user1.getOrganization().getId().equals(user2.getOrganization().getId());
  }

  private boolean isSelf(UserDetails user1, UserDetails user2) {
    return Objects.equals(user1.getUserId(), user2.getUserId());
  }

  private void validateSort(SearchCriteria searchCriteria) {
    if (searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy())) {
      if (!availableSortFields.contains(searchCriteria.getSortBy())) {
        throw new BadRequestException(ProjectService.class, String.format("Invalid %s sortBy field for users", searchCriteria.getSortBy()));
      }
    }
  }

  private boolean isSortActive(SearchCriteria searchCriteria) {
    return StringUtils.isNotEmpty(searchCriteria.getSort()) && StringUtils.isNotEmpty(searchCriteria.getSortBy());
  }

  private User getUser(String uuid, Boolean withRole) {
    ConcurrentMapCache usersCache = (ConcurrentMapCache) cacheManager.getCache(USERS_CACHE);
    if (usersCache != null && usersCache.getNativeCache().size() != 0) {
      User user = (User) usersCache.getNativeCache().get(uuid);
      if (user != null) {
        return user;
      }
    }
    return getUserById(uuid, withRole);
  }

  private User getUser(String userId) {
    try {
      return keycloakFeign.getUser(userId);
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      log.error(LOG_KEYCLOAK_ERROR, e.getMessage(), e.status());
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER,
          String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      log.error(LOG_KEYCLOAK_ERROR_GET_USER, userId, e.getMessage(), e.status());
      throw new ResourceNotFound(UserService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, userId));
    }
  }

  private List<String> getRequestedRoles(String roles) {
    return Arrays.stream(roles.split(","))
                 .collect(Collectors.toList());
  }

  private Map<String, Object> getUserRaw(@NotNull String userId) {
    try {
      Map<String, Object> userRaw = keycloakFeign.getUserRaw(userId);
      if (Objects.isNull(userRaw)) {
        throw new SystemException(UserService.class, FETCHING_USER_FROM_KEYCLOAK_FAILED);
      }
      return userRaw;
    } catch (FeignException fe) {
      log.error(LOG_KEYCLOAK_ERROR_GET_USER, userId, fe.getMessage(), fe.status());
      throw new SystemException(UserService.class, FETCHING_USER_FROM_KEYCLOAK_FAILED);
    }
  }
}
