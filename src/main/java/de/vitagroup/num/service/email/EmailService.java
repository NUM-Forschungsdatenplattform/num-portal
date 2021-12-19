package de.vitagroup.num.service.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class EmailService {

  private final JavaMailSender javaMailSender;
  private final EmailProperties emailProperties;

  public void sendEmail(String subject, String htmlBody, String to) {

    MimeMessage message = javaMailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(emailProperties.getFrom());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      try {
        String text = new BufferedReader(
            new InputStreamReader(message.getInputStream(), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        System.out.println("----------------- EMAIL ----------------------------------------");
        System.out.println(text);
        System.out.println("----------------------------------------------------------------");

      } catch (IOException e) {
        e.printStackTrace();
      }



      log.debug("Message to {} successfully sent", to);
    } catch (MessagingException e) {
      log.error("Sending email to {} failed", to);
    }
  }

  public void sendEmailWithAttachment(
      String subject,
      String contents,
      String to,
      String attachment,
      String filename,
      String contentType) {

    MimeMessage message = javaMailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(emailProperties.getFrom());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(contents);

      helper.addAttachment(
          filename,
          new ByteArrayResource(attachment.getBytes(StandardCharsets.UTF_8)),
          contentType);
      javaMailSender.send(message);
      log.debug("Message to {} successfully sent", to);
    } catch (MessagingException e) {
      log.error("Sending email to {} failed", to);
    }
  }
}
