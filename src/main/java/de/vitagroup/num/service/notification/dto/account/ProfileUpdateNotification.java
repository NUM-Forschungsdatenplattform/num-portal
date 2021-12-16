package de.vitagroup.num.service.notification.dto.account;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import de.vitagroup.num.service.notification.dto.Notification;
import java.time.Year;
import lombok.Builder;

public class ProfileUpdateNotification extends Notification {

  private static final String PROFILE_UPDATE_SUBJECT = "mail.user-profile-update.subject";
  private static final String PROFILE_UPDATE_BODY = "mail.user-profile-update.body";

  @Builder
  public ProfileUpdateNotification(
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
