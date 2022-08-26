package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.time.Year;

public class NewUserNotification extends Notification {

  private static final String NEW_USER_SUBJECT_KEY = "mail.new-user.subject";
  private static final String NEW_USER_BODY_KEY = "mail.new-user.body";

  private static final String TRANSLATION_KEY_PREFIX = "role.";

  private final String newUserEmail;
  private final String newUserFirstName;
  private final String newUserLastName;

  private final String requestedRole;
  private final String department;
  private final String notes;

  @Builder
  public NewUserNotification(
          String newUserEmail,
          String newUserFirstName,
          String newUserLastName,
          String requestedRole, String department,
          String notes, String recipientEmail,
          String recipientFirstName,
          String recipientLastName) {

    this.newUserEmail = newUserEmail;
    this.newUserFirstName = newUserFirstName;
    this.newUserLastName = newUserLastName;
    this.requestedRole = requestedRole;
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
    String translatedRequestedRole = StringUtils.EMPTY;
    if (StringUtils.isNotEmpty(requestedRole)) {
      final String msgKey = TRANSLATION_KEY_PREFIX + requestedRole.toLowerCase();
      translatedRequestedRole = messageSource.getMessage(msgKey);
    }
    return messageSource.getMessage(
        NEW_USER_BODY_KEY,
        recipientFirstName,
        recipientLastName,
        newUserFirstName,
        newUserLastName,
        newUserEmail,
        copyright,
        url,
        translatedRequestedRole, department, notes);
  }
}
