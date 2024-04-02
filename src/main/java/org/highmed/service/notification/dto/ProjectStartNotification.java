package org.highmed.service.notification.dto;

import lombok.Builder;
import org.highmed.service.email.MessageSourceWrapper;

import java.time.Year;

public class ProjectStartNotification extends Notification {

  private static final String PROJECT_START_SUBJECT_KEY = "mail.project-start.subject";
  private static final String PROJECT_START_BODY_KEY = "mail.project-start.body";

  private final String coordinatorFirstName;
  private final String coordinatorLastName;
  private final String projectTitle;
  private final long projectId;

  @Builder
  public ProjectStartNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String coordinatorFirstName,
      String coordinatorLastName,
      String projectTitle,
      long projectId) {

    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.coordinatorFirstName = coordinatorFirstName;
    this.coordinatorLastName = coordinatorLastName;
    this.projectTitle = projectTitle;
    this.projectId = projectId;
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
        url,
        getProjectExplorerUrl(url, projectId));
  }
}
