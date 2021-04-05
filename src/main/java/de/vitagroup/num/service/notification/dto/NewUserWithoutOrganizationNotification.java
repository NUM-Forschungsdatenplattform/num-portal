package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class NewUserWithoutOrganizationNotification extends Notification {

  private final String subjectKey = "mail.user-without-organization.subject";
  private final String subjectBody = "mail.user-without-organization.body";

  private String newUserEmail;
  private String newUserFirstName;
  private String newUserLastName;

  @Builder
  public NewUserWithoutOrganizationNotification(
      String newUserEmail,
      String newUserFirstName,
      String newUserLastName,
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName){
    this.newUserEmail = newUserEmail;
    this.newUserFirstName = newUserFirstName;
    this.newUserLastName = newUserLastName;

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
        subjectBody,
        recipientFirstName,
        recipientLastName,
        newUserFirstName,
        newUserLastName,
        newUserEmail,
        copyright,
        url);
  }
}
