package org.highmed.numportal.service;

import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.OrganizationRepository;
import org.highmed.numportal.domain.repository.UserDetailsRepository;
import org.highmed.numportal.domain.specification.UserDetailsSpecification;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.metric.UsersMetrics;
import org.highmed.numportal.service.notification.NotificationService;
import org.highmed.numportal.service.notification.dto.NewUserNotification;
import org.highmed.numportal.service.notification.dto.NewUserWithoutOrganizationNotification;
import org.highmed.numportal.service.notification.dto.Notification;
import org.highmed.numportal.service.notification.dto.account.AccountApprovalNotification;
import org.highmed.numportal.service.notification.dto.account.AccountStatusChangedNotification;
import org.highmed.numportal.service.notification.dto.account.OrganizationUpdateNotification;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_NOT_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;

@Slf4j
@Service
public class UserDetailsService {

  private static final String USER_ATTRIBUTE_DEPARTMENT = "department";
  private static final String USER_ATTRIBUTE_REQUESTED_ROLE = "requested-role";
  private static final String USER_ATTRIBUTE_ADDITIONAl_NOTES = "notes";
  private static final String FULL_NAME_FORMAT = "%s %s";
  private final UserDetailsRepository userDetailsRepository;
  private final OrganizationRepository organizationRepository;
  private final OrganizationService organizationService;
  private final NotificationService notificationService;
  private final UserService userService;
  private final UsersMetrics usersMetrics;


  @Autowired
  public UserDetailsService(
      @Lazy UserService userService,
      UserDetailsRepository userDetailsRepository,
      OrganizationRepository organizationRepository,
      @Lazy OrganizationService organizationService,
      NotificationService notificationService,
      UsersMetrics usersMetrics) {
    this.userService = userService;
    this.notificationService = notificationService;
    this.organizationService = organizationService;
    this.organizationRepository = organizationRepository;
    this.userDetailsRepository = userDetailsRepository;
    this.usersMetrics = usersMetrics;
  }

  protected void deleteUserDetails(String userId) {
    userDetailsRepository.deleteById(userId);
  }

  public Optional<UserDetails> getUserDetailsById(String userId) {
    return userDetailsRepository.findByUserId(userId);
  }

  public UserDetails createUserDetails(String userId, String emailAddress) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      return userDetails.get();
    } else {
      UserDetails newUserDetails = UserDetails.builder().userId(userId)
                                              .createdDate(LocalDateTime.now(ZoneOffset.UTC)).build();
      organizationService
          .resolveOrganization(emailAddress)
          .ifPresent(newUserDetails::setOrganization);
      UserDetails saved = userDetailsRepository.save(newUserDetails);
      log.info("Create user details on first login for user {}", userId);

      if (saved.getOrganization() != null) {
        notificationService.send(collectOrganizationAdminNotifications(userId));
      } else {
        notificationService.send(collectAdminNotifications(userId));
      }
      // trigger cache update
      userService.addUserToCache(userId);
      usersMetrics.addNewUserAsUnapproved();
      return saved;
    }
  }

  public UserDetails setOrganization(String loggedInUserId, String userId, Long organizationId) {
    checkIsUserApproved(loggedInUserId);

    UserDetails userDetails =
        userDetailsRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFound(UserDetailsService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, userId)));

    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () -> new ResourceNotFound(UserDetailsService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId)));

    if (nonNull(organization.getActive()) && (!organization.getActive())) {
      String logMessage = String.format(CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION, organization.getName());
      log.warn(logMessage);
      throw new ForbiddenException(OrganizationService.class, CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION, logMessage);
    }

    String formerOrganizationName =
        userDetails.getOrganization() != null ? userDetails.getOrganization().getName() : "-";

    userDetails.setOrganization(organization);
    UserDetails saved = userDetailsRepository.save(userDetails);

    List<Notification> notifications = new LinkedList<>();
    notifications.addAll(collectOrganizationAdminNotifications(userId));
    notifications.addAll(
        collectOrganizationUpdateNotification(
            userId, loggedInUserId, organization.getName(), formerOrganizationName));

    notificationService.send(notifications);
    //trigger cache update
    userService.addUserToCache(userId);

    return saved;
  }

  public UserDetails approveUser(String loggedInUserId, String userId) {

    checkIsUserApproved(loggedInUserId);

    UserDetails userDetails =
        userDetailsRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFound(UserDetailsService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, userId)));
    userDetails.setApproved(true);
    UserDetails saved = userDetailsRepository.save(userDetails);

    notificationService.send(collectAccountApprovalNotification(userId, loggedInUserId));
    usersMetrics.approveUser();
    log.info("User {} was approved by {}", userId, loggedInUserId);
    return saved;
  }

  public UserDetails checkIsUserApproved(String userId) {
    UserDetails user =
        getUserDetailsById(userId).orElseThrow(() -> new SystemException(UserDetailsService.class, USER_NOT_FOUND,
            String.format(USER_NOT_FOUND, userId)));

    if (user.isNotApproved()) {
      log.warn("User {} is not approved", userId);
      throw new ForbiddenException(UserDetailsService.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED);
    }

    return user;
  }

  public Page<UserDetails> getUsers(Pageable pageable, UserDetailsSpecification specification) {
    return userDetailsRepository.findAll(specification, pageable);
  }

  public Long countUserDetails() {
    return userDetailsRepository.count();
  }

  public List<String> getAllUsersUUID() {
    return userDetailsRepository.getAllUsersId();
  }

  public Long countUserDetailsByOrganization(Long organizationId) {
    return userDetailsRepository.countByOrganization(organizationId);
  }

  @Transactional
  public void deactivateUsers(String loggedInUserId, Long organizationId) {
    List<UserDetails> users = userDetailsRepository.findByOrganizationId(organizationId);
    for (UserDetails userDetails : users) {
      log.info("Deactivate user {} ", userDetails.getUserId());
      userService.updateUserActiveField(loggedInUserId, userDetails.getUserId(), Boolean.FALSE);
    }
  }

  public void updateUsersInCache(Long organizationId) {
    List<String> userIds = userDetailsRepository.findUserIdsByOrganizationIds(organizationId);
    for (String userId : userIds) {
      userService.addUserToCache(userId);
    }
  }

  public void sendAccountStatusChangedNotification(String userId, String loggedInUserId, Boolean currentStatus) {
    List<Notification> notifications = new LinkedList<>();
    User user = userService.getUserById(userId, false);
    User admin = userService.getUserById(loggedInUserId, false);

    if (user != null && admin != null) {
      AccountStatusChangedNotification statusChangedNotification = AccountStatusChangedNotification.builder()
                                                                                                   .recipientEmail(user.getEmail())
                                                                                                   .recipientFirstName(user.getFirstName())
                                                                                                   .recipientLastName(user.getLastName())
                                                                                                   .adminEmail(admin.getEmail())
                                                                                                   .adminFullName(String.format(FULL_NAME_FORMAT,
                                                                                                       admin.getFirstName(), admin.getLastName()))
                                                                                                   .userCurrentStatus(currentStatus)
                                                                                                   .build();
      notifications.add(statusChangedNotification);
    } else {
      log.warn("Could not create account status changed email notification.");
    }
    notificationService.send(notifications);
  }

  private List<Notification> collectAccountApprovalNotification(
      String userId, String loggedInUserId) {
    List<Notification> notifications = new LinkedList<>();
    User user = userService.getUserById(userId, false);
    User admin = userService.getUserById(loggedInUserId, false);

    if (user != null && admin != null) {
      AccountApprovalNotification not =
          AccountApprovalNotification.builder()
                                     .recipientEmail(user.getEmail())
                                     .recipientFirstName(user.getFirstName())
                                     .recipientLastName(user.getLastName())
                                     .adminEmail(admin.getEmail())
                                     .adminFullName(String.format(FULL_NAME_FORMAT, admin.getFirstName(), admin.getLastName()))
                                     .build();

      notifications.add(not);
    } else {
      log.warn("Could not create account approval email notification.");
    }

    return notifications;
  }

  private List<Notification> collectOrganizationUpdateNotification(
      String userId, String loggedInUserId, String organization, String formerOrganization) {
    List<Notification> notifications = new LinkedList<>();

    User user = userService.getUserById(userId, false);
    User admin = userService.getUserById(loggedInUserId, false);

    if (user != null && admin != null) {
      OrganizationUpdateNotification not =
          OrganizationUpdateNotification.builder()
                                        .recipientEmail(user.getEmail())
                                        .recipientFirstName(user.getFirstName())
                                        .recipientLastName(user.getLastName())
                                        .adminEmail(admin.getEmail())
                                        .adminFullName(String.format(FULL_NAME_FORMAT, admin.getFirstName(), admin.getLastName()))
                                        .organization(organization)
                                        .formerOrganization(formerOrganization)
                                        .build();

      notifications.add(not);
    } else {
      log.warn("Could not create organization update email notification.");
    }
    return notifications;
  }

  private List<Notification> collectOrganizationAdminNotifications(String userId) {
    List<Notification> notifications = new LinkedList<>();

    User user = userService.getUserById(userId, false);

    Set<User> admins = userService.getByRole(Roles.ORGANIZATION_ADMIN);
    admins.removeIf(
        u ->
            u.getOrganization() == null
                || !u.getOrganization().getName().equals(user.getOrganization().getName()));
    List<String> departmentAtrs = getUserAttribute(user, USER_ATTRIBUTE_DEPARTMENT);
    String userDepartment = !departmentAtrs.isEmpty() ? departmentAtrs.get(0) : StringUtils.EMPTY;
    List<String> requestedRoles = getUserAttribute(user, USER_ATTRIBUTE_REQUESTED_ROLE);
    List<String> notesAtrs = getUserAttribute(user, USER_ATTRIBUTE_ADDITIONAl_NOTES);
    String notes = !notesAtrs.isEmpty() ? notesAtrs.get(0) : StringUtils.EMPTY;

    admins.forEach(
        admin -> {
          NewUserNotification notification =
              NewUserNotification.builder()
                                 .newUserEmail(user.getEmail())
                                 .newUserFirstName(user.getFirstName())
                                 .newUserLastName(user.getLastName())
                                 .recipientEmail(admin.getEmail())
                                 .recipientFirstName(admin.getFirstName())
                                 .recipientLastName(admin.getLastName())
                                 .department(userDepartment)
                                 .requestedRoles(requestedRoles)
                                 .notes(notes)
                                 .build();

          notifications.add(notification);
        });

    return notifications;
  }

  private List<Notification> collectAdminNotifications(String userId) {
    List<Notification> notifications = new LinkedList<>();

    User user = userService.getUserById(userId, false);
    Set<User> superAdmins = userService.getByRole(Roles.SUPER_ADMIN);

    superAdmins.forEach(
        admin -> {
          NewUserWithoutOrganizationNotification notification =
              NewUserWithoutOrganizationNotification.builder()
                                                    .userEmail(user.getEmail())
                                                    .userFirstName(user.getFirstName())
                                                    .userLastName(user.getLastName())
                                                    .recipientEmail(admin.getEmail())
                                                    .recipientFirstName(admin.getFirstName())
                                                    .recipientLastName(admin.getLastName())
                                                    .build();
          notifications.add(notification);
        });
    return notifications;
  }

  private List<String> getUserAttribute(User user, String attributeName) {
    return user.getAttributes() != null ? (List<String>) user.getAttributes().getOrDefault(attributeName, Collections.EMPTY_LIST)
        : Collections.EMPTY_LIST;
  }
}
