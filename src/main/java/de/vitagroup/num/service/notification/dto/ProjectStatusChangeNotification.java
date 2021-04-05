package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class ProjectStatusChangeNotification extends Notification {

  private final String subjectKey = "mail.project-status-change.subject";
  private final String bodyKey = "mail.project-status-change.body";

  private String approverFirstName;
  private String approverLastName;
  private String projectTitle;
  private StudyStatus projectStatus;

  @Builder
  public ProjectStatusChangeNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String approverFirstName,
      String approverLastName,
      String projectTitle,
      StudyStatus projectStatus) {

    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.approverFirstName = approverFirstName;
    this.approverLastName = approverLastName;
    this.projectTitle = projectTitle;
    this.projectStatus = projectStatus;
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
        projectTitle,
        projectStatus,
        approverFirstName,
        approverLastName,
        copyright,
        url);
  }
}
