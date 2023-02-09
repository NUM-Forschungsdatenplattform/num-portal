package de.vitagroup.num.service;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import de.vitagroup.num.domain.dto.UserNameDto;
import de.vitagroup.num.domain.specification.UserDetailsSpecification;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.RolesUpdateNotification;
import de.vitagroup.num.service.notification.dto.account.UserNameUpdateNotification;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
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
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.*;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  private static final int MAX_USER_COUNT = 100000;

  private final KeycloakFeign keycloakFeign;

  private final UserDetailsService userDetailsService;

  private final OrganizationMapper organizationMapper;

  private final NotificationService notificationService;

  private final CacheManager cacheManager;

  private static final String USERS_CACHE = "users";

  private static final String KEYCLOACK_DEFAULT_ROLES_PREFIX = "default-roles-";

  private final List<String> availableSortFields = Arrays.asList(FIRST_NAME, LAST_NAME, ORGANIZATION_NAME, REGISTRATION_DATE, MAIL);

  private static final String FIRST_NAME = "firstName";

  private static final String LAST_NAME = "lastName";

  private static final String ORGANIZATION_NAME = "organization";

  private static final String REGISTRATION_DATE = "registrationDate";

  private static final String MAIL = "email";

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
  @CachePut(value = USERS_CACHE, key="#uuid")
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
   * Retrieves user details without roles - used for determining the ownership on aql, projects and
   * comments; caches results
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
    } catch (ResourceNotFound e){
      return null;
    }
  }

  /**
   * Retrieves user, portal user details and corresponding roles from identity provider
   *
   * @param userId External id of the user
   * @return User
   */
  @Transactional
  public User getUserById(String userId, Boolean withRole) {
    try {
      User user = keycloakFeign.getUser(userId);

      if (BooleanUtils.isTrue(withRole)) {
        addRoles(user);
      }

      addUserDetails(user);
      return user;

    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      log.warn("User not found in keycloak: {}", userId);
      throw new ResourceNotFound(UserService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, userId));
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

  /**
   * Assigns roles to a particular user
   *
   * @param userId    the user to update roles of
   * @param roleNames The list of roles of the user
   */
  public List<String> setUserRoles(
      String userId,
      @NotNull List<String> roleNames,
      String callerUserId,
      List<String> callerRoles) {

    UserDetails userToChange =
        userDetailsService
            .getUserDetailsById(userId)
            .orElseThrow(() -> new SystemException(UserService.class, USER_NOT_FOUND,
                    String.format(USER_NOT_FOUND, userId)));

    UserDetails callerDetails = userDetailsService.checkIsUserApproved(callerUserId);

    if (callerRoles.contains(Roles.ORGANIZATION_ADMIN)
        && !callerRoles.contains(Roles.SUPER_ADMIN)
        && !callerDetails
        .getOrganization()
        .getId()
        .equals(userToChange.getOrganization().getId())) {
      throw new ForbiddenException(UserService.class, ORGANIZATION_ADMIN_CAN_ONLY_MANAGE_USERS_IN_THEIR_OWN_ORGANIZATION);
    }

    Role[] removeRoles;
    Role[] addRoles;
    try {
      Map<String, Role> supportedRoles =
          keycloakFeign.getRoles().stream().collect(Collectors.toMap(Role::getName, role -> role));

      Set<Role> existingRoles = keycloakFeign.getRolesOfUser(userId);

      removeRoles =
          existingRoles.stream()
              .filter(role -> !roleNames.contains(role.getName()))
              .collect(Collectors.toList())
              .toArray(new Role[] {});

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
              .collect(Collectors.toList())
              .toArray(new Role[] {});

      if (Arrays.stream(removeRoles)
          .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException(UserService.class, NOT_ALLOWED_TO_REMOVE_THAT_ROLE);
      }
      if (Arrays.stream(addRoles)
          .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException(UserService.class, NOT_ALLOWED_TO_SET_THAT_ROLE);
      }

      if (removeRoles.length > 0) {
        keycloakFeign.removeRoles(userId, removeRoles);
      }

      if (addRoles.length > 0) {
        keycloakFeign.addRoles(userId, addRoles);
      }

    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER,
              String.format(AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound(UserService.class, ROLE_OR_USER_NOT_FOUND);
    }

    Set<Role> current = keycloakFeign.getRolesOfUser(userId);

    notificationService.send(
        collectRolesUpdateNotification(userId, callerUserId, addRoles, removeRoles, current));
    return roleNames;
  }

  private Set<Role> getUserRoles(String userId) {
    try {
      return keycloakFeign.getRolesOfUser(userId);
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound(UserService.class, NO_ROLES_FOUND);
    }
  }

  private Set<String> getUserRoleNames(String userId) {
    try {
      return keycloakFeign.getRolesOfUser(userId).stream().map(Role::getName).collect(Collectors.toSet());
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound(UserService.class, NO_ROLES_FOUND);
    }
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
   * @param approved  Indicates that the user has been approved by the admin
   * @param search    A string contained in username, first or last name, or email
   * @param withRoles flag whether to add roles to the user structure, if present, or not
   * @return the users that match the search parameters and with optional roles if indicated
   */
  public Set<User> searchUsers(
      String loggedInUserId,
      Boolean approved,
      String search,
      Boolean withRoles,
      List<String> callerRoles) {

    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);

    Set<User> users = keycloakFeign.searchUsers(search, MAX_USER_COUNT);
    if (users == null) {
      return Collections.emptySet();
    }
    users.removeIf(u -> userDetailsService.getUserDetailsById(u.getId()).isEmpty());
    users.forEach(this::addUserDetails);

    if ((withRoles != null && withRoles) || Roles.isProjectLead(callerRoles)) {
      users.forEach(this::addRoles);
    }

    if (approved != null) {
      users.removeIf(user -> approved ? user.isNotApproved() : user.isApproved());
    }

    return filterByCallerRole(users, callerRoles, loggedInUser);
  }

  @Transactional
  public Page<User> searchUsersWithPagination(String loggedInUserId, List<String> callerRoles, SearchCriteria searchCriteria, Pageable pageable) {

    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);
    validateSort(searchCriteria);

    Set<String> usersUUID = new HashSet<>();
    boolean searchCriteriaProvided = searchCriteria.getFilter() != null &&
            searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY);
    if (searchCriteriaProvided) {
      String searchValue = searchCriteria.getFilter() != null &&
              searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY) ? (String) searchCriteria.getFilter().get(SearchCriteria.FILTER_SEARCH_BY_KEY) : null;
      usersUUID = this.findUsersUUID(searchValue);
    }
    if (CollectionUtils.isEmpty(usersUUID) && searchCriteriaProvided) {
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

    Boolean withRoles = searchCriteria.getFilter() != null &&
            searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_USER_WITH_ROLES_KEY) ?
            Boolean.valueOf((String) searchCriteria.getFilter().get(SearchCriteria.FILTER_USER_WITH_ROLES_KEY)) : null;
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

  private UserDetailsSpecification buildUserSpecification(UserDetails loggedInUser, List<String> callerRoles, SearchCriteria searchCriteria, Set<String> usersUUID) {
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
    UserDetailsSpecification userDetailsSpecification = UserDetailsSpecification.builder()
            .approved(approved)
            .loggedInUserOrganizationId(organizationId)
            .usersUUID(usersUUID)
            .build();
    return userDetailsSpecification;
  }

  private void sortUsers(List<User> users, SearchCriteria searchCriteria) {
    String field = searchCriteria.getSortBy() != null ? searchCriteria.getSortBy() : REGISTRATION_DATE;
    Sort.Direction sortOrder = searchCriteria.getSort() != null ?
            Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()) : Sort.Direction.DESC;
    Comparator<User> userComparator = getComparator(field);
    if (sortOrder.isAscending()) {
      Collections.sort(users, Comparator.nullsLast(userComparator));
    } else {
      Collections.sort(users, Comparator.nullsLast(userComparator.reversed()));
    }
  }

  private Comparator<User> getComparator(String field) {
    switch (field) {
      case FIRST_NAME:
        return Comparator.comparing(u -> u.getFirstName().toUpperCase());
      case LAST_NAME:
        return Comparator.comparing(u -> u.getLastName().toUpperCase());
      case ORGANIZATION_NAME:
        return Comparator.comparing(user -> user.getOrganization() != null ? user.getOrganization().getName().toUpperCase() : StringUtils.EMPTY);
      case REGISTRATION_DATE:
        return Comparator.comparing(User::getCreatedTimestamp);
      case MAIL:
        return Comparator.comparing(u -> u.getEmail().toUpperCase());
      default:
        return Comparator.comparing(User::getCreatedTimestamp);
    }
  }

  @Transactional
  public Set<User> getByRole(String role) {
    Set<User> users = keycloakFeign.getByRole(role);
    users.removeIf(u -> userDetailsService.getUserDetailsById(u.getId()).isEmpty());
    users.forEach(this::addUserDetails);
    return users;
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

    if (user != null && admin != null) {
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

  private Set<User> filterByCallerRole(
      Set<User> users, List<String> callerRoles, UserDetails loggedInUser) {
    if (callerRoles.contains(Roles.SUPER_ADMIN)) {
      return users;
    }

    Set<User> outputSet = new HashSet<>();

    if (Roles.isOrganizationAdmin(callerRoles) && loggedInUser.getOrganization() != null) {
      Long loggedInOrgId = loggedInUser.getOrganization().getId();
      users.forEach(
          user -> {
            log.info("Processed user {} ", user);
            if (user.getOrganization() != null
                && loggedInOrgId.equals(user.getOrganization().getId())) {
              outputSet.add(user);
            }
          });
    }

    if (Roles.isProjectLead(callerRoles)) {
      users.forEach(
          user -> {
            if (user.getRoles() != null && user.getRoles().contains(Roles.RESEARCHER)) {
              outputSet.add(user);
            }
          });
    }

    return outputSet;
  }

  private void deleteNotVerifiedUser(String userId) {
    try {
      User user = keycloakFeign.getUser(userId);
      if (user != null && BooleanUtils.isFalse(user.getEmailVerified())) {
        keycloakFeign.deleteUser(userId);
      } else {
        throw new BadRequestException(UserService.class, CANNOT_DELETE_ENABLED_USER);
      }
    } catch (Exception e) {
      throw new SystemException(UserService.class, AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER,
              String.format(AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER, e.getMessage()));
    }
  }

  /**
   * Evicts users cache every 8 hours
   */
  @Scheduled(fixedRate = 28800000)
  public void evictParametersCache() {
    var cache = cacheManager.getCache(USERS_CACHE);
    if (cache != null) {
      log.trace("Evicting users cache");
      cache.clear();
    }
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
    } else {
      throw new ForbiddenException(UserService.class,
              CAN_ONLY_CHANGE_OWN_NAME_ORG_ADMIN_NAMES_OF_THE_PEOPLE_IN_THE_ORGANIZATION_AND_SUPERUSER_ALL_NAMES);
    }

    notificationService.send(collectUserNameUpdateNotification(userIdToChange, loggedInUserId));
    return getUserById(userIdToChange, true);
  }

  /**
   * Retrieved a list of users UUID that match the search criteria
   * @param search A string contained in username, first or last name, or email
   * @return
   */
  public Set<String> findUsersUUID(String search) {
    Set<String> userUUIDs = new HashSet<>();
    ConcurrentMapCache usersCache = (ConcurrentMapCache) cacheManager.getCache(USERS_CACHE);
    if (usersCache != null && usersCache.getNativeCache().size() != 0) {
      ConcurrentMap<Object, Object> users = usersCache.getNativeCache();
      for (Map.Entry<Object, Object> entry : users.entrySet()) {
        if (entry.getValue() instanceof User) {
          User user = (User) entry.getValue();
          if (StringUtils.containsIgnoreCase(user.getFullName(), search) || StringUtils.containsIgnoreCase(user.getEmail(), search)) {
            userUUIDs.add((String) entry.getKey());
          }
        }
      }
      return userUUIDs;
    }
    return Collections.emptySet();
  }

  private void updateName(String userId, UserNameDto userNameDto) {
    Map<String, Object> userRaw = keycloakFeign.getUserRaw(userId);
    if (userRaw == null) {
      throw new SystemException(UserService.class, FETCHING_USER_FROM_KEYCLOAK_FAILED);
    }
    userRaw.put(FIRST_NAME, userNameDto.getFirstName());
    userRaw.put(LAST_NAME, userNameDto.getLastName());
    keycloakFeign.updateUser(userId, userRaw);
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
        throw new BadRequestException(ProjectService.class, String.format("Invalid %s sortBy field for projects", searchCriteria.getSortBy()));
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
}
