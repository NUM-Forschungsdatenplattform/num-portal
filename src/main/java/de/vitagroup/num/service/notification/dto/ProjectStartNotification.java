package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class ProjectStartNotification extends Notification {

  private final String subjectKey = "mail.project-start.subject";
  private final String bodyKey = "mail.project-start.body";

  private String coordinatorFirstName;
  private String coordinatorLastName;
  private String projectTitle;

  @Builder
  public ProjectStartNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String coordinatorFirstName,
      String coordinatorLastName,
      String projectTitle) {

    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.coordinatorFirstName = coordinatorFirstName;
    this.coordinatorLastName = coordinatorLastName;
    this.projectTitle = projectTitle;
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(subjectKey);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(copyrightKey, Year.now());
    return messageSource.getMessage(
        bodyKey,
        recipientFirstName,
        recipientLastName,
        coordinatorFirstName,
        coordinatorLastName,
        projectTitle,
        copyright,
        url);
  }
}
