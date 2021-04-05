package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class UserUpdateNotification extends Notification {

  private final String subjectKey = "mail.user-update.subject";
  private final String subjectBody = "mail.user-update.body";

  @Builder
  public UserUpdateNotification(
      String recipientEmail, String recipientFirstName, String recipientLastName) {
    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(subjectKey);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(copyrightKey, Year.now());
    return messageSource.getMessage(
        subjectBody, recipientFirstName, recipientLastName, copyright, url);
  }
}
