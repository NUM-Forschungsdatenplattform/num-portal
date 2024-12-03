package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.UserDetailsDto;
import org.highmed.numportal.domain.model.admin.UserDetails;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@AllArgsConstructor
public class UserDetailsMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public UserDetailsDto convertToDto(UserDetails userDetails) {
    return modelMapper.map(userDetails, UserDetailsDto.class);
  }

  public UserDetails convertToEntity(UserDetailsDto dto) {
    return modelMapper.map(dto, UserDetails.class);
  }
}
