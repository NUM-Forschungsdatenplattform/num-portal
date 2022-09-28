package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import lombok.Builder;

import java.time.Year;
import java.util.Iterator;
import java.util.List;

public class NewUserNotification extends Notification {

  private static final String NEW_USER_SUBJECT_KEY = "mail.new-user.subject";
  private static final String NEW_USER_BODY_KEY = "mail.new-user.body";

  private static final String TRANSLATION_KEY_PREFIX = "role.";

  private final String newUserEmail;
  private final String newUserFirstName;
  private final String newUserLastName;

  private final List<String> requestedRoles;
  private final String department;
  private final String notes;

  @Builder
  public NewUserNotification(
          String newUserEmail,
          String newUserFirstName,
          String newUserLastName,
          List<String> requestedRoles, String department,
          String notes, String recipientEmail,
          String recipientFirstName,
          String recipientLastName) {

    this.newUserEmail = newUserEmail;
    this.newUserFirstName = newUserFirstName;
    this.newUserLastName = newUserLastName;
    this.requestedRoles = requestedRoles;
    this.department = department;
    this.notes = notes;

    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(NEW_USER_SUBJECT_KEY);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        NEW_USER_BODY_KEY,
        recipientFirstName,
        recipientLastName,
        newUserFirstName,
        newUserLastName,
        newUserEmail,
        copyright,
        url,
        getTranslatedRequestedRoles(requestedRoles, messageSource), department, notes);
  }

  private String getTranslatedRequestedRoles(List<String> roles, MessageSourceWrapper messageSource) {
    Iterator<String> iter = roles.iterator();
    StringBuilder sb = new StringBuilder();

    while (iter.hasNext()) {
      final String msgKey = TRANSLATION_KEY_PREFIX + iter.next().toLowerCase();
      String translatedRequestedRole = messageSource.getMessage(msgKey);
      sb.append(translatedRequestedRole).append(iter.hasNext() ? ", " : "");
    }
    return sb.toString();
  }
}
