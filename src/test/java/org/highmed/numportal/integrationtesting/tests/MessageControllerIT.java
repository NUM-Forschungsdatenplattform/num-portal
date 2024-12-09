package org.highmed.numportal.integrationtesting.tests;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.MessageType;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MessageControllerIT extends IntegrationTest{

  private static final String MESSAGE_PATH = "/message";

  private ObjectMapper mapper = new ObjectMapper()
      .registerModule(new PageJacksonModule())
      .registerModule(new SortJacksonModule())
      .registerModule(new JavaTimeModule());

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
            .startDate(now.minusHours(15))
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

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"CONTENT_ADMIN"})
  public void getUserMessages (){

    MvcResult result =
        mockMvc.perform(get(MESSAGE_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    String contentAsString = result.getResponse().getContentAsString();
    Page page = mapper.readValue(contentAsString, Page.class);
    List content = page.getContent();

    Assert.assertEquals(3, page.getTotalElements());
    MessageDto firstMessage = mapper.convertValue(content.get(0), MessageDto.class);
    Assert.assertEquals("Inactive message", firstMessage.getTitle());
    Assert.assertEquals(MessageType.ERROR, firstMessage.getType());
  }

}
