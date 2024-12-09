package org.highmed.numportal.integrationtesting.tests;

import org.highmed.numportal.TestNumPortalApplication;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.MessageType;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestNumPortalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("itest")
public class MessageControllerIT extends IntegrationTest{

  private static final String MESSAGE_PATH = "/message";

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  private MessageRepository messageRepository;

  @Before
  public void setUpMessage(){
//    messageRepository.deleteAll();
    LocalDateTime now = java.time.LocalDateTime.now().minusMinutes(5);
    Message inactiveMessage =
        Message.builder()
            .title("Inactive message")
            .type(MessageType.ERROR)
            .startDate(now.minusHours(10))
            .endDate(now.minusHours(5))
            .build();
    messageRepository.save(inactiveMessage);
    Message activeMessage =
        Message.builder()
               .title("Active message")
               .type(MessageType.INFO)
               .startDate(now.minusHours(10))
               .endDate(now.plusMinutes(5))
               .build();
    messageRepository.save(activeMessage);
    Message plannedMessage =
        Message.builder()
               .title("Planned message")
               .type(MessageType.INFO)
               .startDate(now.plusHours(1))
               .endDate(now.plusHours(10))
               .build();
    messageRepository.save(plannedMessage);
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"CONTENT_ADMIN"})
  public void getUserMessages (){

    MvcResult result =
        mockMvc.perform(get(MESSAGE_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    Assert.assertEquals(3, result.getResponse().getContentAsByteArray().length);
  }

}
