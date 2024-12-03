package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.MessageType;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.mapper.MessageMapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageServiceTest {

  private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

  @Mock
  private MessageRepository messageRepository;
  @Mock
  private MessageMapper messageMapper;
  @Mock
  private UserDetailsService userDetailsService;

  @InjectMocks
  private MessageService messageService;

  @Test
  public void createUserMessageTest() {
    MessageDto messageDto = MessageDto.builder()
                                      .title("Other title")
                                      .text("Hier koennte deine Nachricht stehen")
                                      .startDate(LocalDateTime.now())
                                      .endDate(LocalDateTime.MAX)
                                      .type(MessageType.INFO).build();
    Message message = Message.builder()
                                   .title("Other title")
                                   .text("Hier koennte deine Nachricht stehen")
                                   .startDate(LocalDateTime.now())
                                   .endDate(LocalDateTime.MAX)
                                   .type(MessageType.INFO).build();

    when(messageMapper.convertToEntity(messageDto)).thenReturn(message);
    when(messageRepository.save(message)).thenReturn(message);
    when(messageMapper.convertToDTO(message)).thenReturn(messageDto);
    MessageDto returnMessage = messageService.createUserMessage(messageDto, USER_ID);

    Assert.assertEquals(messageDto, returnMessage);

    Mockito.verify(messageMapper, Mockito.times(1)).convertToEntity(messageDto);
    Mockito.verify(messageMapper, Mockito.times(1)).convertToDTO(message);
    Mockito.verify(messageRepository, Mockito.times(1)).save(message);
    Mockito.verify(userDetailsService, Mockito.times(1)).checkIsUserApproved(USER_ID);
  }
}
