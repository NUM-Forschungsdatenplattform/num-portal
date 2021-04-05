package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class UserUpdateNotification extends Notification {

  private static final String SUBJECT_KEY = "mail.user-update.subject";
  private static final String BODY_KEY = "mail.user-update.body";

  @Builder
  public UserUpdateNotification(
      String recipientEmail, String recipientFirstName, String recipientLastName) {
    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(SUBJECT_KEY);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        BODY_KEY, recipientFirstName, recipientLastName, copyright, url);
  }
}
