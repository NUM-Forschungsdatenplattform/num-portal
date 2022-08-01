package de.vitagroup.num.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.properties.NumProperties;
import de.vitagroup.num.service.email.EmailService;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import de.vitagroup.num.service.notification.dto.*;
import de.vitagroup.num.service.notification.dto.account.AccountApprovalNotification;
import de.vitagroup.num.service.notification.dto.account.UserNameUpdateNotification;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

  @Mock private EmailService emailService;

  @Mock private MessageSourceWrapper messageSource;

  @Mock private NumProperties numProperties;

  @InjectMocks private NotificationService notificationService;

  @Before
  public void setUp() {
    when((messageSource.getMessage(any()))).thenReturn("Any content");
    when((messageSource.getMessage(anyString(), any()))).thenReturn("Any content");
    when((messageSource.getMessage(any(), any(), any(), any(), any(), any(),any()))).thenReturn("Any content");
  }

  @Test
  public void shouldSendOneEmailPerNotification() {
    when((numProperties.getUrl())).thenReturn("Portal url");

    notificationService.send(
        List.of(
            AccountApprovalNotification.builder().recipientEmail("john.doe@vita.ag").build(),
            UserNameUpdateNotification.builder().recipientEmail("jane.doe@vita.ag").build()));

    verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
  }

  @Test
  public void shouldSendEmailsPerNotification() {
    when((numProperties.getUrl())).thenReturn("Portal url");

    notificationService.send(
        List.of(
            ProjectStartNotification.builder().recipientEmail("john.doe@vita.ag").build(),
            ProjectCloseNotification.builder().recipientEmail("jane.doe@vita.ag").build(),
            ProjectStatusChangeNotification.builder().recipientEmail("anne.doe@vita.ag").build(),
            ProjectApprovalRequestNotification.builder()
                .recipientEmail("ann.doe@vita.ag")
                .build()));

    verify(emailService, times(4)).sendEmail(anyString(), anyString(), anyString());
  }

  @Test
  public void shouldNotSendEmailsWhenRecipientMissing() {
    when((numProperties.getUrl())).thenReturn("Portal url");

    notificationService.send(
        List.of(
            ProjectStartNotification.builder().build(),
            ProjectCloseNotification.builder().build(),
            ProjectStatusChangeNotification.builder().build(),
            ProjectApprovalRequestNotification.builder()
                .recipientEmail("ann.doe@vita.ag")
                .build()));

    verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
  }

  @Test
  public void shouldCorrectlyComputeProjectPreviewUrl() {
    String portalUrl = "https://staging.num-codex.de/home";

    Notification not = ProjectStartNotification.builder().build();
    String previewUrl = not.getProjectPreviewUrl(portalUrl, 124L);
    String expectedPreviewUrl = "https://staging.num-codex.de/projects/124/editor?mode=preview";

    assertEquals(expectedPreviewUrl, previewUrl);
  }

  @Test
  public void shouldCorrectlyComputeProjectExplorerUrl() {
    String portalUrl = "https://staging.num-codex.de/home";

    Notification not = ProjectStatusChangeNotification.builder().build();
    String explorerUrl = not.getProjectExplorerUrl(portalUrl, 8L);
    String expectedExplorerUrl = "https://staging.num-codex.de/data-explorer/projects/8";

    assertEquals(explorerUrl, expectedExplorerUrl);
  }

  @Test
  public void shouldCorrectlyComputeProjectReviewUrl() {
    String portalUrl = "https://staging.num-codex.de/home";

    Notification not = ProjectStartNotification.builder().build();
    String reviewUrl = not.getProjectReviewUrl(portalUrl, 9L);
    String expectedReviewUrl = "https://staging.num-codex.de/projects/9/editor?mode=review";

    assertEquals(expectedReviewUrl, reviewUrl);
  }

  @Test
  public void shouldCorrectlyComputeProjectEditUrl() {
    String portalUrl = "https://staging.num-codex.de/home";
    Notification not = ProjectStatusChangeRequestNotification.builder()
            .recipientEmail("recipient-test@num-codex.de")
            .build();
    String editUrl = not.getProjectEditUrl(portalUrl, 9L);
    String expectedEditUrl = "https://staging.num-codex.de/projects/9/editor?mode=edit";
    assertEquals(expectedEditUrl, editUrl);
  }
}
