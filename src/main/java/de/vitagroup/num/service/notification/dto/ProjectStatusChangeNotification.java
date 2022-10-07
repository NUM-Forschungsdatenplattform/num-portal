package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import java.time.Year;
import lombok.Builder;

public class ProjectStatusChangeNotification extends Notification {

  private static final String PROJECT_STATUS_CHANGE_SUBJECT_KEY =
      "mail.project-status-change.subject";
  protected static final String PROJECT_STATUS_CHANGE_BODY_KEY = "mail.project-status-change.body";

  protected final String approverFirstName;
  protected final String approverLastName;
  protected final String approverEmail;
  protected final String projectTitle;
  protected final ProjectStatus projectStatus;
  protected final ProjectStatus oldProjectStatus;
  protected final long projectId;

  @Builder
  public ProjectStatusChangeNotification(
      String recipientEmail,
      String recipientFirstName,
      String recipientLastName,
      String approverFirstName,
      String approverLastName,
      String projectTitle,
      ProjectStatus projectStatus,
      ProjectStatus oldProjectStatus,
      long projectId, String approverEmail) {

    this.recipientEmail = recipientEmail;
    this.recipientFirstName = recipientFirstName;
    this.recipientLastName = recipientLastName;
    this.approverFirstName = approverFirstName;
    this.approverLastName = approverLastName;
    this.projectTitle = projectTitle;
    this.projectStatus = projectStatus;
    this.oldProjectStatus = oldProjectStatus;
    this.projectId = projectId;
    this.approverEmail = approverEmail;
  }

  @Override
  public String getNotificationSubject(MessageSourceWrapper messageSource) {
    return messageSource.getMessage(PROJECT_STATUS_CHANGE_SUBJECT_KEY);
  }

  @Override
  public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
    String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
    return messageSource.getMessage(
        PROJECT_STATUS_CHANGE_BODY_KEY,
        recipientFirstName,
        recipientLastName,
        projectTitle,
        projectStatus,
        approverFirstName,
        approverLastName,
        copyright,
        url,
        getProjectPreviewUrl(url, projectId),
        oldProjectStatus,
        approverEmail);
  }
}
