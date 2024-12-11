package org.highmed.numportal.integrationtesting.tests;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.MessageType;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MessageControllerIT extends IntegrationTest {

  private static final String MESSAGE_PATH = "/message";
  private final ObjectMapper mapper = new ObjectMapper()
      .registerModule(new PageJacksonModule())
      .registerModule(new SortJacksonModule())
      .registerModule(new JavaTimeModule());
  private Long updateMessageId;

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  private MessageRepository messageRepository;

  @Before
  public void setUpMessage() {
    messageRepository.deleteAll();
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
    Message save = messageRepository.save(plannedMessage);
    updateMessageId = save.getId();
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"CONTENT_ADMIN"})
  public void createUserMessage() {
    LocalDateTime start = java.time.LocalDateTime.now().plusMinutes(5);
    Message plannedMessage =
        Message.builder()
            .title("Planned message")
            .text("This is a <strong>strong</strong> message, with <script>evil code injection</script>.")
            .type(MessageType.INFO)
            .startDate(start)
            .endDate(start.plusMonths(10))
            .build();

    mockMvc.perform(post(MESSAGE_PATH).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(plannedMessage)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(plannedMessage.getTitle()))
        .andExpect(jsonPath("$.text", not(containsString("<script>"))))
        .andReturn();
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"CONTENT_ADMIN"})
  public void updateUserMessage() {
    LocalDateTime start = java.time.LocalDateTime.now().plusHours(1);
    Message updateMessage =
        Message.builder()
            .title("Update planned message")
            .text("This is a <strong>strong</strong> message, with <script>evil code injection</script>.")
            .type(MessageType.INFO)
            .startDate(start)
            .endDate(start.plusMonths(10))
            .build();

    mockMvc.perform(put(MESSAGE_PATH + "/{id}", updateMessageId).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(updateMessage)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(updateMessage.getTitle()))
        .andExpect(jsonPath("$.text", not(containsString("<script>"))))
        .andReturn();
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"CONTENT_ADMIN"})
  @SuppressWarnings("rawtypes")
  public void getUserMessages() {
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

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"MANAGER"})
  public void noAccessApiWithWrongRole() {
    LocalDateTime start = java.time.LocalDateTime.now().plusMinutes(5);
    Message someMessage =
        Message.builder()
            .title("Planned message")
            .type(MessageType.INFO)
            .startDate(start)
            .endDate(start.plusMonths(10))
            .build();
    mockMvc.perform(post(MESSAGE_PATH).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(someMessage))).andExpect(status().isForbidden());
    mockMvc.perform(get(MESSAGE_PATH).with(csrf())).andExpect(status().isForbidden());
    mockMvc.perform(put(MESSAGE_PATH + "/{id}", 3).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(someMessage))).andExpect(status().isForbidden());
    mockMvc.perform(patch(MESSAGE_PATH + "/{id}", 3).with(csrf())).andExpect(status().isForbidden());
    mockMvc.perform(delete(MESSAGE_PATH + "/{id}", 3).with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  public void markUserMessageAsReadTest(){
    mockMvc.perform(post(MESSAGE_PATH + "/read/{id}", 2).with(csrf()))
           .andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  public void getAllDisplayedUserMessagesTest(){
    MvcResult result = mockMvc.perform(get(MESSAGE_PATH + "/read").with(csrf()))
           .andExpect(status().isOk()).andReturn();
    List<UserDetails> readUserMessageList = mapper.readValue(result.getResponse().getContentAsString(), List.class);
    Assert.assertEquals(1, readUserMessageList.size());
  }
}
