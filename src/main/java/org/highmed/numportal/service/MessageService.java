package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.mapper.MessageMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

  private MessageMapper messageMapper;
  private UserDetailsService userDetailsService;

  private MessageRepository messageRepository;

  public MessageDto createUserMessage(MessageDto messageDto, String userId) {
    Message message = messageMapper.convertToEntity(messageDto);
    userDetailsService.checkIsUserApproved(userId);

    Message savedMessage = messageRepository.save(message);

    return messageMapper.convertToDTO(savedMessage);
  }

}
