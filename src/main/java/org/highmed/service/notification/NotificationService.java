package org.highmed.service.notification;

import org.highmed.properties.NumProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.highmed.service.email.EmailService;
import org.highmed.service.email.MessageSourceWrapper;
import org.highmed.service.notification.dto.Notification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

  private final EmailService emailService;

  private final MessageSourceWrapper messageSource;

  private final NumProperties numProperties;

  @Async
  public void send(List<Notification> notifications) {
    notifications.forEach(
        notification -> {
          String subject = notification.getNotificationSubject(messageSource);
          String body = notification.getNotificationBody(messageSource, numProperties.getUrl());
          String recipient = notification.getNotificationRecipient();

          if (StringUtils.isNotEmpty(body)
              && StringUtils.isNotEmpty(subject)
              && StringUtils.isNotEmpty(recipient)) {
            emailService.sendEmail(subject, body, recipient);
          } else {
            log.warn("Could not send notification");
          }
        });
  }
}
