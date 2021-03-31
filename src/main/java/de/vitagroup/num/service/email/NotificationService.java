package de.vitagroup.num.service.email;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.properties.NumProperties;
import de.vitagroup.num.service.UserService;
import java.time.Year;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final EmailService emailService;
  private final MessageSourceWrapper messageSource;
  private final UserService userService;
  private final NumProperties properties;

  private String copyright;

  @PostConstruct
  public void after() {
    copyright = messageSource.getMessage("num.copyright", Year.now());
  }

  @Async
  public void notify(Notification notification) {

    switch (notification.getType()) {
      case NEW_USER_WITHOUT_ORGANIZATION:
        notifyAdmins(notification);
        break;
      case NEW_USER:
        notifyOrganizationAdmin(notification);
        break;
      case USER_UPDATE:
        notifyUser(notification);
        break;
      case PROJECT_START:
      case PROJECT_CLOSED:
        notifyResearchers(notification);
        break;
      case PROJECT_STATUS_CHANGE:
        notifyCoordinator(notification);
        break;
      case PROJECT_PENDING_APPROVAL:
        notifyAppovers(notification);
        break;
    }
  }

  private void notifyAppovers(Notification notification) {
    User coordinator = userService.getUserById(notification.getCoordinator().getUserId(), false);
    Set<User> approvers = userService.getByRole(Roles.STUDY_APPROVER);

    String subject = messageSource.getMessage(notification.getSubject());

    approvers.forEach(
        approver -> {
          String body =
              messageSource.getMessage(
                  notification.getBody(),
                  approver.getFirstName(),
                  approver.getLastName(),
                  coordinator.getFirstName(),
                  coordinator.getLastName(),
                  notification.getProjectTitle(),
                  copyright,
                  properties.getUrl());
          emailService.sendEmail(subject, body, approver.getEmail());
        });
  }

  private void notifyCoordinator(Notification notification) {
    User approver = userService.getUserById(notification.getApprover().getUserId(), false);
    User coordinator = userService.getUserById(notification.getCoordinator().getUserId(), false);

    String subject = messageSource.getMessage(notification.getSubject());

    String body =
        messageSource.getMessage(
            notification.getBody(),
            coordinator.getFirstName(),
            coordinator.getLastName(),
            notification.getProjectTitle(),
            notification.getStatus(),
            approver.getFirstName(),
            approver.getLastName(),
            copyright,
            properties.getUrl());

    emailService.sendEmail(subject, body, coordinator.getEmail());
  }

  private void notifyAdmins(Notification notification) {
    User user = userService.getUserById(notification.getUserId(), false);
    Set<User> admins = userService.getByRole(Roles.SUPER_ADMIN);

    String subject = messageSource.getMessage(notification.getSubject());

    admins.forEach(
        admin -> {
          String body =
              messageSource.getMessage(
                  notification.getBody(),
                  admin.getFirstName(),
                  admin.getLastName(),
                  user.getFirstName(),
                  user.getLastName(),
                  user.getEmail(),
                  copyright,
                  properties.getUrl());
          emailService.sendEmail(subject, body, admin.getEmail());
        });
  }

  private void notifyOrganizationAdmin(Notification notification) {
    User user = userService.getUserById(notification.getUserId(), false);

    Set<User> admins = userService.getByRole(Roles.ORGANIZATION_ADMIN);
    admins.removeIf(
        u ->
            u.getOrganization() == null
                || !u.getOrganization().getName().equals(user.getOrganization().getName()));

    admins.forEach(
        admin -> {
          String body =
              messageSource.getMessage(
                  notification.getBody(),
                  admin.getFirstName(),
                  admin.getLastName(),
                  user.getFirstName(),
                  user.getLastName(),
                  user.getEmail(),
                  copyright,
                  properties.getUrl());

          emailService.sendEmail(
              messageSource.getMessage(notification.getSubject()), body, admin.getEmail());
        });
  }

  private void notifyUser(Notification notification) {
    User user = userService.getUserById(notification.getUserId(), false);

    String body =
        messageSource.getMessage(
            notification.getBody(),
            user.getFirstName(),
            user.getLastName(),
            copyright,
            properties.getUrl());

    emailService.sendEmail(
        messageSource.getMessage(notification.getSubject()), body, user.getEmail());
  }

  private void notifyResearchers(Notification notification) {
    List<UserDetails> users = notification.getResearchers();

    User coordinator = userService.getUserById(notification.getCoordinator().getUserId(), false);

    users.forEach(
        userDetails -> {
          User user = userService.getUserById(userDetails.getUserId(), false);
          String body =
              messageSource.getMessage(
                  notification.getBody(),
                  user.getFirstName(),
                  user.getLastName(),
                  coordinator.getFirstName(),
                  coordinator.getLastName(),
                  notification.getProjectTitle(),
                  copyright,
                  properties.getUrl());

          emailService.sendEmail(
              messageSource.getMessage(notification.getSubject()), body, user.getEmail());
        });
  }
}
