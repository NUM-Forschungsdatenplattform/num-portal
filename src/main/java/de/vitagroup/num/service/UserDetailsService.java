package de.vitagroup.num.service;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.MailDomainRepository;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.NewUserNotification;
import de.vitagroup.num.service.notification.dto.NewUserWithoutOrganizationNotification;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.UserUpdateNotification;
import de.vitagroup.num.web.exception.ConflictException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsService {

  public static final String DOMAIN_SEPARATOR = "@";

  private UserDetailsRepository userDetailsRepository;
  private OrganizationRepository organizationRepository;
  private MailDomainRepository mailDomainRepository;
  private NotificationService notificationService;
  private UserService userService;

  @Autowired
  public UserDetailsService(
      @Lazy UserService userService,
      UserDetailsRepository userDetailsRepository,
      OrganizationRepository organizationRepository,
      MailDomainRepository mailDomainRepository,
      NotificationService notificationService) {
    this.userService = userService;
    this.notificationService = notificationService;
    this.mailDomainRepository = mailDomainRepository;
    this.organizationRepository = organizationRepository;
    this.userDetailsRepository = userDetailsRepository;
  }

  public Optional<UserDetails> getUserDetailsById(String userId) {
    return userDetailsRepository.findByUserId(userId);
  }

  public UserDetails createUserDetails(String userId, String emailAddress) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      throw new ConflictException("User " + userId + " already exists.");
    } else {
      UserDetails newUserDetails = UserDetails.builder().userId(userId).build();
      resolveOrganization(emailAddress).ifPresent(newUserDetails::setOrganization);
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

    userDetails.setOrganization(organization);
    UserDetails saved = userDetailsRepository.save(userDetails);

    notificationService.send(collectOrganizationAdminNotifications(userId));

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

    notificationService.send(collectUserNotifications(userId));

    return saved;
  }

  public UserDetails checkIsUserApproved(String userId) {
    UserDetails user =
        getUserDetailsById(userId).orElseThrow(() -> new SystemException("User not found"));

    if (user.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. User is not approved.");
    }

    return user;
  }

  private Optional<Organization> resolveOrganization(String email) {
    if (StringUtils.isBlank(email) || !email.contains(DOMAIN_SEPARATOR)) {
      return Optional.empty();
    }
    String domain = email.split("\\" + DOMAIN_SEPARATOR)[1];
    Optional<MailDomain> mailDomain = mailDomainRepository.findByName(domain.toLowerCase());
    return mailDomain.map(MailDomain::getOrganization);
  }

  private List<Notification> collectUserNotifications(String userId) {
    List<Notification> notifications = new LinkedList<>();
    User user = userService.getUserById(userId, false);

    UserUpdateNotification not =
        UserUpdateNotification.builder()
            .recipientEmail(user.getEmail())
            .recipientFirstName(user.getFirstName())
            .recipientLastName(user.getLastName())
            .build();

    notifications.add(not);

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
          NewUserNotification not =
              NewUserNotification.builder()
                  .newUserEmail(user.getEmail())
                  .newUserFirstName(user.getFirstName())
                  .newUserLastName(user.getLastName())
                  .recipientEmail(admin.getEmail())
                  .recipientFirstName(admin.getFirstName())
                  .recipientLastName(admin.getLastName())
                  .build();

          notifications.add(not);
        });

    return notifications;
  }

  private List<Notification> collectAdminNotifications(String userId) {
    List<Notification> notifications = new LinkedList<>();

    User user = userService.getUserById(userId, false);
    Set<User> superAdmins = userService.getByRole(Roles.SUPER_ADMIN);

    superAdmins.forEach(
        admin -> {
          NewUserWithoutOrganizationNotification not =
              NewUserWithoutOrganizationNotification.builder()
                  .newUserEmail(user.getEmail())
                  .newUserFirstName(user.getFirstName())
                  .newUserLastName(user.getLastName())
                  .recipientEmail(admin.getEmail())
                  .recipientFirstName(admin.getFirstName())
                  .recipientLastName(admin.getLastName())
                  .build();
          notifications.add(not);
        });
    return notifications;
  }
}
