package org.highmed.numportal.service.notification.dto;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.highmed.numportal.service.email.MessageSourceWrapper;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public abstract class Notification {

  protected static final String COPYRIGHT_KEY = "num.copyright";

  protected static final String PROJECT_PREVIEW_MODE = "/editor?mode=preview";

  protected static final String PROJECT_REVIEW_MODE = "/editor?mode=review";

  protected static final String PROJECT_EDIT_MODE = "/editor?mode=edit";

  protected String recipientEmail;
  protected String recipientFirstName;
  protected String recipientLastName;

  protected String adminEmail;
  protected String adminFullName;

  public String getNotificationRecipient() {
    return recipientEmail;
  }

  public abstract String getNotificationBody(MessageSourceWrapper messageSource, String url);

  public abstract String getNotificationSubject(MessageSourceWrapper messageSource);

  public String getProjectPreviewUrl(String portalUrl, Long projectId) {
    return getProjectUrl(portalUrl, projectId, PROJECT_PREVIEW_MODE);
  }

  public String getProjectReviewUrl(String portalUrl, Long projectId) {
    return getProjectUrl(portalUrl, projectId, PROJECT_REVIEW_MODE);
  }

  public String getProjectEditUrl(String portalUrl, Long projectId) {
    return getProjectUrl(portalUrl, projectId, PROJECT_EDIT_MODE);
  }

  public String getProjectExplorerUrl(String portalUrl, Long projectId) {
    String baseUrl = getBaseUrl(portalUrl);
    if (StringUtils.isNotEmpty(baseUrl)) {
      return String.format("%s%s%d", baseUrl, "/data-explorer/projects/", projectId);
    } else {
      return "-";
    }
  }

  private String getBaseUrl(String portalUrl) {
    if (StringUtils.isNotEmpty(portalUrl)) {
      try {
        URL url = new URL(portalUrl);
        return url.getProtocol() + "://" + url.getHost();
      } catch (MalformedURLException e) {
        log.warn("Cannot extract base url");
      }
    }
    return StringUtils.EMPTY;
  }

  private String getProjectUrl(String portalUrl, Long projectId, String mode) {
    String baseUrl = getBaseUrl(portalUrl);
    if (StringUtils.isNotEmpty(baseUrl)) {
      return String.format("%s%s%d%s", baseUrl, "/projects/", projectId, mode);
    } else {
      return "-";
    }
  }
}
