package org.highmed.service.notification.dto.account;

import lombok.Builder;
import org.highmed.service.email.MessageSourceWrapper;
import org.highmed.service.notification.dto.Notification;

import java.time.Year;

public class OrganizationUpdateNotification extends Notification {

  private static final String ORGANIZATION_UPDATE_SUBJECT = "mail.user-organization-update.subject";
  private static final String ORGANIZATION_UPDATE_BODY = "mail.user-organization-update.body";
  private final String organization;
  private final String formerOrganization;

  @Builder
  public OrganizationUpdateNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String adminEmail,
      String adminFullName,
      String organization,
      String formerOrganization) {
    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.adminFullName = adminFullName;
    this.adminEmail = adminEmail;
    this.organization = organization;
    this.formerOrganization = formerOrganization;
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        ORGANIZATION_UPDATE_BODY,
        recipientFirstName,
        recipientLastName,
        copyright,
        url,
        adminFullName,
        adminEmail,
        organization,
        formerOrganization);
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(ORGANIZATION_UPDATE_SUBJECT);
  }
}
