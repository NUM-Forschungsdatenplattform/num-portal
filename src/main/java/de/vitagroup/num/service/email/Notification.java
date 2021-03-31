package de.vitagroup.num.service.email;

import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class Notification {

  private NotificationType type;

  private String userId;
  private String subject;
  private String body;
  private String projectTitle;
  private List<UserDetails> researchers;
  private UserDetails coordinator;
  private UserDetails approver;
  private StudyStatus status;

  @Builder
  public Notification(
      NotificationType type,
      String userId,
      String projectTitle,
      List<UserDetails> researchers,
      UserDetails coordinator,
      UserDetails approver,
      StudyStatus status) {
    this.type = type;
    this.userId = userId;
    this.projectTitle = projectTitle;
    this.researchers = researchers;
    this.coordinator = coordinator;
    this.approver = approver;
    this.status = status;

    switch (type) {
      case NEW_USER_WITHOUT_ORGANIZATION:
        {
          subject = "mail.user-without-organization.subject";
          body = "mail.user-without-organization.body";
          break;
        }
      case NEW_USER:
        {
          subject = "mail.new-user.subject";
          body = "mail.new-user.body";
          break;
        }
      case PROJECT_START:
        {
          subject = "mail.project-start.subject";
          body = "mail.project-start.body";
          break;
        }
      case USER_UPDATE:
        {
          subject = "mail.user-update.subject";
          body = "mail.user-update.body";
          break;
        }
      case PROJECT_CLOSED:
        {
          subject = "mail.project-close.subject";
          body = "mail.project-close.body";
          break;
        }
      case PROJECT_STATUS_CHANGE:
        {
          subject = "mail.project-status-change.subject";
          body = "mail.project-status-change.body";
          break;
        }
      case PROJECT_PENDING_APPROVAL:
        {
          subject = "mail.project-pending-approval.subject";
          body = "mail.project-pending-approval.body";
          break;
        }
    }
  }
}
