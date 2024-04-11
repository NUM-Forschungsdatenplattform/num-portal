package org.highmed.numportal.service.email;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.highmed.numportal.service.email.EmailProperties;
import org.highmed.numportal.service.email.EmailService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage message;

    @Before
    public void setup() {
        Mockito.when(emailProperties.getFrom()).thenReturn("support@highmed.org");
        message = new MimeMessage((Session) null);
        Mockito.when(javaMailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    public void sendEmailTest() throws MessagingException {
        emailService.sendEmail("Test subject", "test body", "testaccount@highmed.org");
        Assert.assertEquals("testaccount@highmed.org", message.getRecipients(Message.RecipientType.TO)[0].toString());
        Assert.assertEquals("Test subject", message.getSubject());
    }

    @Test
    public void sendEmailWithAttachmentTest() throws MessagingException {
        emailService.sendEmailWithAttachment("another test subject", "dummy email body", "testaccount2@highmed.org", "dummy file content", "dummyAttachment.txt","text/plain");
        Assert.assertEquals("testaccount2@highmed.org", message.getRecipients(Message.RecipientType.TO)[0].toString());
        Assert.assertEquals("another test subject", message.getSubject());
        Assert.assertEquals("text/plain", message.getContentType());
    }
}
