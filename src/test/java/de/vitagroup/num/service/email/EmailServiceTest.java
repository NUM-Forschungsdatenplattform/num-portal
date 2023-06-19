package de.vitagroup.num.service.email;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

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
        Mockito.when(emailProperties.getFrom()).thenReturn("support@vitagroup.de");
        message = new MimeMessage((Session) null);
        Mockito.when(javaMailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    public void sendEmailTest() throws MessagingException {
        emailService.sendEmail("Test subject", "test body", "testaccount@vitagroup.de");
        Assert.assertEquals("testaccount@vitagroup.de", message.getRecipients(Message.RecipientType.TO)[0].toString());
        Assert.assertEquals("Test subject", message.getSubject());
    }

    @Test
    public void sendEmailWithAttachmentTest() throws MessagingException {
        emailService.sendEmailWithAttachment("another test subject", "dummy email body", "testaccount2@vitagroup.de", "dummy file content", "dummyAttachment.txt","text/plain");
        Assert.assertEquals("testaccount2@vitagroup.de", message.getRecipients(Message.RecipientType.TO)[0].toString());
        Assert.assertEquals("another test subject", message.getSubject());
        Assert.assertEquals("text/plain", message.getContentType());
    }
}
