package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.MessageRepository;
import org.highmed.numportal.domain.repository.UserDetailsRepository;
import org.highmed.numportal.mapper.MessageMapper;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_MESSAGE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_HANDLE_DATE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_UPDATE_MESSAGE_INVALID;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.MESSAGE_NOT_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;


@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

  private static final Safelist safeList = Safelist.simpleText().addTags("br");
  private final UserDetailsRepository userDetailsRepository;

  private MessageMapper messageMapper;
  private UserDetailsService userDetailsService;

  private MessageRepository messageRepository;

  public MessageDto createUserMessage(MessageDto messageDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    Message message = messageMapper.convertToEntity(messageDto);
    if (message.getText() != null && !message.getText().isBlank()) {
      message.setText(Jsoup.clean(message.getText(), safeList));
    }
    validateDates(message.getStartDate(), message.getEndDate(), LocalDateTime.now().minusMinutes(5));

    Message savedMessage = messageRepository.save(message);

    return messageMapper.convertToDto(savedMessage);
  }

  public Page<MessageDto> getMessages(String userId, Pageable pageable) {
    userDetailsService.checkIsUserApproved(userId);
    Page<Message> messagePage = messageRepository.findAll(pageable);
    return messagePage.map(message -> messageMapper.convertToDto(message));
  }

  public MessageDto updateUserMessage(Long id, MessageDto messageDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    LocalDateTime now = LocalDateTime.now().minusMinutes(5);
    Message messageToUpdate = messageRepository.findById(id)
                                               .orElseThrow(() -> new ResourceNotFound(MessageService.class, MESSAGE_NOT_FOUND,
                                                   String.format(MESSAGE_NOT_FOUND, id)));

    if (isInactiveMessage(messageToUpdate, now)) {
      throw new BadRequestException(MessageService.class, CANNOT_UPDATE_MESSAGE_INVALID,
          String.format(CANNOT_UPDATE_MESSAGE_INVALID, messageToUpdate.getId(), "message is not shown anymore"));
    }
    if (isActiveMessage(messageDto, messageToUpdate, now)) {
      messageToUpdate.setEndDate(messageDto.getEndDate());
    } else {
      validateDates(messageDto.getStartDate(), messageDto.getEndDate(), now);

      messageToUpdate.setTitle(messageDto.getTitle());
      if (messageToUpdate.getText() != null && !messageToUpdate.getText().isBlank()) {
        messageToUpdate.setText(Jsoup.clean(messageToUpdate.getText(), safeList));
      }
      messageToUpdate.setStartDate(messageDto.getStartDate());
      messageToUpdate.setEndDate(messageDto.getEndDate());
      messageToUpdate.setType(messageDto.getType());
      messageToUpdate.setSessionBased(messageDto.isSessionBased());

    }
    Message savedMessage = messageRepository.save(messageToUpdate);
    return messageMapper.convertToDto(savedMessage);
  }

  public void deleteActiveUserMessage(Long id, String userId) {
    LocalDateTime now = LocalDateTime.now().minusMinutes(5);
    userDetailsService.checkIsUserApproved(userId);
    Message messageToDelete = messageRepository.findById(id)
                                               .orElseThrow(() -> new ResourceNotFound(MessageService.class, MESSAGE_NOT_FOUND,
                                                   String.format(MESSAGE_NOT_FOUND, id)));
    // active messages should be marked as deleted
    if (messageToDelete.getStartDate().isBefore(now) && messageToDelete.getEndDate().isAfter(now)) {
      messageToDelete.setMarkAsDeleted(true);
      messageRepository.save(messageToDelete);
    } else {
      throw new BadRequestException(MessageService.class, CANNOT_DELETE_MESSAGE,
          String.format(CANNOT_DELETE_MESSAGE, messageToDelete.getId()));
    }
  }

  public void deleteUserMessage(Long id, String userId) {
    LocalDateTime now = LocalDateTime.now().minusMinutes(5);
    userDetailsService.checkIsUserApproved(userId);
    Message messageToDelete = messageRepository.findById(id)
                                               .orElseThrow(() -> new ResourceNotFound(MessageService.class, MESSAGE_NOT_FOUND,
                                                   String.format(MESSAGE_NOT_FOUND, id)));
    //just planned messages can be deleted
    if (messageToDelete.getStartDate().isAfter(now)) {
      messageRepository.deleteById(id);
    } else {
      throw new BadRequestException(MessageService.class, CANNOT_DELETE_MESSAGE,
          String.format(CANNOT_DELETE_MESSAGE, messageToDelete.getId()));
    }
  }

  public void markUserMessageAsRead(Long id, String userId) {
    LocalDateTime now = LocalDateTime.now();
    Message readMessage = messageRepository.findById(id)
                                           .orElseThrow(() -> new ResourceNotFound(MessageService.class, MESSAGE_NOT_FOUND,
                                               String.format(MESSAGE_NOT_FOUND, id)));
    if (readMessage.getStartDate().isBefore(now) && readMessage.getEndDate().isAfter(now) && !readMessage.isSessionBased()) {
      UserDetails userDetails = userDetailsRepository.findByUserId(userId)
                                                    .orElseThrow(() -> new ResourceNotFound(MessageService.class, USER_NOT_FOUND,
                                                        String.format(USER_NOT_FOUND, userId)));
      readMessage.getReadByUsers().add(userDetails);
      messageRepository.save(readMessage);
    } else {
      throw new ForbiddenException(MessageService.class, CANNOT_ACCESS_THIS_RESOURCE);
    }
  }

  public List<MessageDto> getAllDisplayedUserMessages(String userId) {
    UserDetails userDetails = userDetailsRepository.findByUserId(userId)
                                                   .orElseThrow(() -> new ResourceNotFound(MessageService.class, USER_NOT_FOUND,
                                                       String.format(USER_NOT_FOUND, userId)));
    List<Message> notReadByUserMessages = messageRepository.findAllActiveMessagesNotReadByUser(userDetails, LocalDateTime.now());
    List<MessageDto> notReadByUserMessagesDto = new ArrayList<>();
    for (Message message : notReadByUserMessages) {
      MessageDto messageDto = messageMapper.convertToDto(message);
      notReadByUserMessagesDto.add(messageDto);
    }
    return notReadByUserMessagesDto;
  }

  private static boolean isInactiveMessage(Message messageToUpdate, LocalDateTime now) {
    return messageToUpdate.getEndDate().isBefore(now);
  }

  private static boolean isActiveMessage(MessageDto messageDto, Message messageToUpdate, LocalDateTime now) {
    return messageToUpdate.getStartDate().isBefore(now) && messageDto.getEndDate().isAfter(now);
  }

  private void validateDates(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime now) {
    if (startDate.isBefore(now)) {
      throw new BadRequestException(MessageService.class, CANNOT_HANDLE_DATE,
          String.format(CANNOT_HANDLE_DATE, startDate));
    }
    if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
      throw new BadRequestException(MessageService.class, CANNOT_HANDLE_DATE,
          String.format(CANNOT_HANDLE_DATE, endDate));
    }
  }
}
