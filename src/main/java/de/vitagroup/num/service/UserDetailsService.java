package de.vitagroup.num.service;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.NewUserNotification;
import de.vitagroup.num.service.notification.dto.NewUserWithoutOrganizationNotification;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.AccountApprovalNotification;
import de.vitagroup.num.service.notification.dto.account.OrganizationUpdateNotification;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDetailsService {

  private UserDetailsRepository userDetailsRepository;
  private OrganizationRepository organizationRepository;
  private OrganizationService organizationService;
  private NotificationService notificationService;
  private UserService userService;

  @Autowired
  public UserDetailsService(
      @Lazy UserService userService,
      UserDetailsRepository userDetailsRepository,
      OrganizationRepository organizationRepository,
      @Lazy OrganizationService organizationService,
      NotificationService notificationService) {
    this.userService = userService;
    this.notificationService = notificationService;
    this.organizationService = organizationService;
    this.organizationRepository = organizationRepository;
    this.userDetailsRepository = userDetailsRepository;
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
      UserDetails newUserDetails = UserDetails.builder().userId(userId).build();
      organizationService
          .resolveOrganization(emailAddress)
          .ifPresent(newUserDetails::setOrganization);
      UserDetails saved = userDetailsRepository.save(newUserDetails);

      if (saved.getOrganization() != null) {
        notificationService.send(collectOrganizationAdminNotifications(userId));
      } else {
        notificationService.send(collectAdminNotifications(userId));
      }
      return saved;
    }
  }

  public UserDetails setOrganization(String loggedInUserId, String userId, Long organizationId) {
    checkIsUserApproved(loggedInUserId);

    UserDetails userDetails =
        userDetailsRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFound("User not found:" + userId));

    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new ResourceNotFound("Organization not found:" + organizationId));

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

    return saved;
  }

  public UserDetails approveUser(String loggedInUserId, String userId) {

    checkIsUserApproved(loggedInUserId);

    UserDetails userDetails =
        userDetailsRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFound("User not found:" + userId));
    userDetails.setApproved(true);
    UserDetails saved = userDetailsRepository.save(userDetails);

    notificationService.send(collectAccountApprovalNotification(userId, loggedInUserId));

    return saved;
  }

  public UserDetails checkIsUserApproved(String userId) {
    UserDetails user =
        getUserDetailsById(userId).orElseThrow(() -> new SystemException("User not found"));

    if (user.isNotApproved()) {
      log.warn("User {} is not approved", userId);
      throw new ForbiddenException("Cannot access this resource. User is not approved.");
    }

    return user;
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
              .adminFullName(String.format("%s %s", admin.getFirstName(), admin.getLastName()))
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
              .adminFullName(String.format("%s %s", admin.getFirstName(), admin.getLastName()))
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
}
