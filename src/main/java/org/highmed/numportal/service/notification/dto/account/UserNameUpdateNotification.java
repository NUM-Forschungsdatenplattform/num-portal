package org.highmed.numportal.service.notification.dto.account;

import lombok.Builder;
import org.highmed.numportal.service.email.MessageSourceWrapper;
import org.highmed.numportal.service.notification.dto.Notification;

import java.time.Year;

public class UserNameUpdateNotification extends Notification {

  private static final String PROFILE_UPDATE_SUBJECT = "mail.user-profile-update.subject";
  private static final String PROFILE_UPDATE_BODY = "mail.user-profile-update.body";

  @Builder
  public UserNameUpdateNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String adminEmail,
      String adminFullName) {
    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.adminFullName = adminFullName;
    this.adminEmail = adminEmail;
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        PROFILE_UPDATE_BODY,
        recipientFirstName,
        recipientLastName,
        copyright,
        url,
        adminFullName,
        adminEmail);
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(PROFILE_UPDATE_SUBJECT);
  }
}
