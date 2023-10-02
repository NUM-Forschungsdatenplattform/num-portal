package de.vitagroup.num.service.notification.dto;

import de.vitagroup.num.domain.model.ProjectStatus;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import lombok.Builder;

import java.time.Year;

public class ProjectStatusChangeRequestNotification extends ProjectStatusChangeNotification {

    @Builder(builderMethodName = "changeRequestBuilder")
    public ProjectStatusChangeRequestNotification(String recipientEmail, String recipientFirstName,
                                                  String recipientLastName, String approverFirstName,
                                                  String approverLastName, String projectTitle,
                                                  ProjectStatus projectStatus, ProjectStatus oldProjectStatus,
                                                  long projectId, String approverEmail) {
        super(recipientEmail, recipientFirstName, recipientLastName, approverFirstName, approverLastName, projectTitle, projectStatus, oldProjectStatus, projectId, approverEmail);
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
                getProjectEditUrl(url, projectId),
                oldProjectStatus,
                approverEmail);
    }
}
