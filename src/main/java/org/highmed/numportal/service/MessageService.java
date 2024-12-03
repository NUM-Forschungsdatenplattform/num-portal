package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.mapper.MessageMapper;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ResourceNotFound;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_UPDATE_MESSAGE_INVALID;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.MESSAGE_NOT_FOUND;


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
    validateDates(message.getStartDate(), message.getEndDate());
    Message savedMessage = messageRepository.save(message);

    return messageMapper.convertToDTO(savedMessage);
  }

  public MessageDto updateUserMessage(Long id, MessageDto messageDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    Message messageToUpdate = messageRepository.findById(id)
                                               .orElseThrow(() -> new ResourceNotFound(MessageService.class, MESSAGE_NOT_FOUND,
                                                   String.format(MESSAGE_NOT_FOUND, id)));
    validateDates(messageDto.getStartDate(), messageDto.getEndDate());

    //Todo: Gibt es neben dem Datum weitere Einschränkungen, wann eine Message geupdated werden kann
    messageToUpdate.setTitle(messageDto.getTitle());
    messageToUpdate.setText(messageDto.getText());
    messageToUpdate.setStartDate(messageDto.getStartDate());
    messageToUpdate.setEndDate(messageDto.getEndDate());
    messageToUpdate.setType(messageDto.getType());

    Message savedMessage = messageRepository.save(messageToUpdate);
    return messageMapper.convertToDTO(savedMessage);
  }

  private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
    if (startDate == null && endDate == null) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    if (startDate != null && startDate.isBefore(now)) {
      throw new BadRequestException(MessageService.class, CANNOT_UPDATE_MESSAGE_INVALID);
    }
    if (endDate != null && endDate.isBefore(now)) {
      throw new BadRequestException(MessageService.class, CANNOT_UPDATE_MESSAGE_INVALID);
    }
    if (startDate != null && endDate != null
        && (startDate.isAfter(endDate) || startDate.isEqual(endDate))) {
      throw new BadRequestException(MessageService.class, CANNOT_UPDATE_MESSAGE_INVALID);
    }
  }
}
