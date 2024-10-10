package org.highmed.numportal.service.notification.dto.account;

import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.service.email.MessageSourceWrapper;
import org.highmed.numportal.service.notification.dto.Notification;

import lombok.Builder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Year;
import java.util.HashMap;
import java.util.List;

public class RolesUpdateNotification extends Notification {

  private static final String USER_ROLES_UPDATE_SUBJECT_KEY = "mail.user-roles-update.subject";
  private static final String USER_ROLES_UPDATE_BODY_KEY = "mail.user-roles.body";

  private static final String OPEN_LIST_HTML_TAG = "<ul>";
  private static final String CLOSE_LIST_HTML_TAG = "</ul>";
  private static final String OPEN_LIST_ELEMENT_HTML_TAG = "<li>";
  private static final String CLOSE_LIST_ELEMENT_HTML_TAG = "</li>";
  private static final String HYPHEN = "-";

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
    if (CollectionUtils.isNotEmpty(roles)) {
      message.append(OPEN_LIST_HTML_TAG);
    }

    roles.forEach(
        role -> {
          String displayName = translationKeys.containsKey(role) ? messageSource.getMessage(translationKeys.get(role)) : StringUtils.EMPTY;
          message.append(OPEN_LIST_ELEMENT_HTML_TAG);
          if (StringUtils.isNotEmpty(displayName)) {
            message.append(displayName);
          } else {
            message.append(role);
          }
          message.append(CLOSE_LIST_ELEMENT_HTML_TAG);
        });

    if (StringUtils.isEmpty(message)) {
      message.append(HYPHEN);
    }
    if (CollectionUtils.isNotEmpty(roles)) {
      message.append(CLOSE_LIST_HTML_TAG);
    }

    return message.toString();
  }
}
