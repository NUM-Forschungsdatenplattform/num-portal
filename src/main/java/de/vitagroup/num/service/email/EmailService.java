/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.service.email;

import java.nio.charset.StandardCharsets;
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

  public void sendEmailWithAttachment(
      String subject,
      String contents,
      String to,
      String attachment,
      String filename,
      String contentType) {

    MimeMessage message2 = javaMailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message2, true);

      helper.setFrom(emailProperties.getFrom());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(contents);

      helper.addAttachment(
          filename,
          new ByteArrayResource(attachment.getBytes(StandardCharsets.UTF_8)),
          contentType);
      javaMailSender.send(message2);
      log.debug("Message to {} successfully sent", to);
    } catch (MessagingException e) {
      log.error("Sending email to {} failed", to);
    }
  }
}
