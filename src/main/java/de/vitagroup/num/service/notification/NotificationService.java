package de.vitagroup.num.service.notification;

import de.vitagroup.num.properties.NumProperties;
import de.vitagroup.num.service.email.EmailService;
import de.vitagroup.num.service.email.MessageSourceWrapper;
import de.vitagroup.num.service.notification.dto.Notification;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NotificationService {

  private final EmailService emailService;

  private final MessageSourceWrapper messageSource;

  private final NumProperties numProperties;

  @Async
  public void send(List<Notification> notifications) {
    notifications.forEach(
        notification -> emailService.sendEmail(
            notification.getNotificationSubject(messageSource),
            notification.getNotificationBody(messageSource, numProperties.getUrl()),
            notification.getNotificationRecipient()));
  }
}
