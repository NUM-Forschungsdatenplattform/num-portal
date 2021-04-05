package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;

public abstract class Notification {

  protected static final String COPYRIGHT_KEY = "num.copyright";

  protected String recipientEmail;
  protected String recipientFirstName;
  protected String recipientLastName;

  public String getNotificationRecipient() {
    return recipientEmail;
  }

  public abstract String getNotificationBody(MessageSourceWrapper messageSource, String url);

  public abstract String getNotificationSubject(MessageSourceWrapper messageSource);
}
