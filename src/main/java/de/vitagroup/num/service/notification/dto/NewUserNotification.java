package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;


public class NewUserNotification extends Notification {

  private static final String SUBJECT_KEY = "mail.new-user.subject";
  private static final String BODY_KEY = "mail.new-user.body";

  private String newUserEmail;
  private String newUserFirstName;
  private String newUserLastName;

  @Builder
  public NewUserNotification(
      String newUserEmail,
      String newUserFirstName,
      String newUserLastName,
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName) {

    this.newUserEmail = newUserEmail;
    this.newUserFirstName = newUserFirstName;
    this.newUserLastName = newUserLastName;

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
        BODY_KEY,
        recipientFirstName,
        recipientLastName,
        newUserFirstName,
        newUserLastName,
        newUserEmail,
        copyright,
        url);
  }
}
