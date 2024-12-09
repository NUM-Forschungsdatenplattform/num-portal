package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.domain.model.Message;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@AllArgsConstructor
public class MessageMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  private void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public MessageDto convertToDto(Message message) {
    return modelMapper.map(message, MessageDto.class);

  }

  public Message convertToEntity(MessageDto messageDto) {
    return modelMapper.map(messageDto, Message.class);
  }
}
