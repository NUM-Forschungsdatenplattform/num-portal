package de.vitagroup.num.service.notification.dto.account;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import de.vitagroup.num.service.notification.dto.Notification;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

public class RolesUpdateNotification extends Notification {

  private static final String USER_ROLES_UPDATE_SUBJECT_KEY = "mail.user-roles-update.subject";
  private static final String USER_ROLES_UPDATE_BODY_KEY = "mail.user-roles.body";

  private final List<String> rolesRemoved;
  private final List<String> rolesAdded;
  private final List<String> allRoles;

  private final HashMap<String, String> translationKeys = new HashMap<>();

  {
    translationKeys.put(Roles.SUPER_ADMIN, "role.super-admin");
    translationKeys.put(Roles.ORGANIZATION_ADMIN, "role.organization-admin");
    translationKeys.put(Roles.CONTENT_ADMIN, "role.content-admin");
    translationKeys.put(Roles.RESEARCHER, "role.researcher");
    translationKeys.put(Roles.STUDY_COORDINATOR, "role.study_coordinator");
    translationKeys.put(Roles.STUDY_APPROVER, "role.study-approver");
    translationKeys.put(Roles.MANAGER, "role.manager");
    translationKeys.put(Roles.CRITERIA_EDITOR, "role.criteria_editor");
  }

  @Builder
  public RolesUpdateNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String adminEmail,
      String adminFullName,
      List<String> rolesRemoved,
      List<String> rolesAdded,
      List<String> allRoles) {
    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.adminEmail = adminEmail;
    this.adminFullName = adminFullName;
    this.rolesRemoved = rolesRemoved;
    this.rolesAdded = rolesAdded;
    this.allRoles = allRoles;
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());

    return messageSource.getMessage(
        USER_ROLES_UPDATE_BODY_KEY,
        recipientFirstName,
        recipientLastName,
        copyright,
        url,
        adminFullName,
        adminEmail,
        getRolesDisplayString(rolesRemoved, messageSource),
        getRolesDisplayString(rolesAdded, messageSource),
        getRolesDisplayString(allRoles, messageSource));
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(USER_ROLES_UPDATE_SUBJECT_KEY);
  }

  private String getRolesDisplayString(List<String> roles, MessageSourceWrapper messageSource) {
    StringBuilder message = new StringBuilder();

    roles.forEach(
        role -> {
          String displayName = messageSource.getMessage(translationKeys.get(role));
          if (StringUtils.isNotEmpty(displayName)) {
            message.append(displayName);
          } else {
            message.append(role);
          }
          message.append(StringUtils.SPACE);
        });

    if (StringUtils.isEmpty(message)) {
      message.append("-");
    }

    return message.toString();
  }
}
