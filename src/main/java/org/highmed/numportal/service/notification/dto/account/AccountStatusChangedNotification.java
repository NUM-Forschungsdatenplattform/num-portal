package org.highmed.numportal.service.notification.dto.account;

import lombok.Builder;
import org.highmed.numportal.service.email.MessageSourceWrapper;
import org.highmed.numportal.service.notification.dto.Notification;

import java.time.Year;

public class AccountStatusChangedNotification extends Notification {

    private static final String ACCOUNT_ACTIVE_SUBJECT = "mail.user-account-active.subject";
    private static final String ACCOUNT_ACTIVE_BODY = "mail.user-account-active-body";
    private static final String ACCOUNT_INACTIVE_SUBJECT = "mail.user-account-inactive.subject";
    private static final String ACCOUNT_INACTIVE_BODY = "mail.user-account-inactive.body";
    private Boolean userCurrentStatus;

    @Builder
    public AccountStatusChangedNotification(
            String recipientEmail,
            String recipientFirstName,
            String recipientLastName,
            String adminEmail,
            String adminFullName,
            Boolean userCurrentStatus) {
        this.recipientEmail = recipientEmail;
        this.recipientFirstName = recipientFirstName;
        this.recipientLastName = recipientLastName;
        this.adminFullName = adminFullName;
        this.adminEmail = adminEmail;
        this.userCurrentStatus = userCurrentStatus;
    }

    @Override
    public String getNotificationBody(MessageSourceWrapper messageSource, String url) {
        String copyright = messageSource.getMessage(COPYRIGHT_KEY, Year.now());
        String messageKey;
        if (Boolean.TRUE.equals(userCurrentStatus)) {
            messageKey = ACCOUNT_ACTIVE_BODY;
        } else {
            messageKey = ACCOUNT_INACTIVE_BODY;
        }
        return messageSource.getMessage(
                messageKey,
                recipientFirstName,
                recipientLastName,
                url,
                adminEmail,
                adminFullName,
                copyright);
    }

    @Override
    public String getNotificationSubject(MessageSourceWrapper messageSource) {
        if (Boolean.TRUE.equals(userCurrentStatus)) {
            return messageSource.getMessage(ACCOUNT_ACTIVE_SUBJECT);
        }
        return messageSource.getMessage(ACCOUNT_INACTIVE_SUBJECT);
    }
}
