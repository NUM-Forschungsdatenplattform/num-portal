package de.vitagroup.num.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.properties.NumProperties;
import de.vitagroup.num.service.email.EmailService;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import de.vitagroup.num.service.notification.dto.account.AccountApprovalNotification;
import de.vitagroup.num.service.notification.dto.account.ProfileUpdateNotification;
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
            ProfileUpdateNotification.builder().recipientEmail("jane.doe@vita.ag").build()));

    verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
  }
}
