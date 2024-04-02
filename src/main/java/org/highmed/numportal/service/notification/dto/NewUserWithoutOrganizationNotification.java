package org.highmed.numportal.service.notification.dto;

import lombok.Builder;
import org.highmed.numportal.service.email.MessageSourceWrapper;

import java.time.Year;

public class NewUserWithoutOrganizationNotification extends Notification {

  private static final String SUBJECT_KEY = "mail.user-without-organization.subject";
  private static final String BODY_KEY = "mail.user-without-organization.body";

  private final String userEmail;
  private final String userFirstName;
  private final String userLastName;

  @Builder
  public NewUserWithoutOrganizationNotification(
      String userEmail,
      String userFirstName,
      String userLastName,
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName){
    this.userEmail = userEmail;
    this.userFirstName = userFirstName;
    this.userLastName = userLastName;

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
        userFirstName,
        userLastName,
        userEmail,
        copyright,
        url);
  }
}
