package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class ProjectStartNotification extends Notification {

  private static final String PROJECT_START_SUBJECT_KEY = "mail.project-start.subject";
  private static final String PROJECT_START_BODY_KEY = "mail.project-start.body";

  private final String coordinatorFirstName;
  private final String coordinatorLastName;
  private final String projectTitle;

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
    return messageSource.getMessage(PROJECT_START_SUBJECT_KEY);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        PROJECT_START_BODY_KEY,
        recipientFirstName,
        recipientLastName,
        coordinatorFirstName,
        coordinatorLastName,
        projectTitle,
        copyright,
        url);
  }
}
