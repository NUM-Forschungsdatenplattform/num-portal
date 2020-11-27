package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

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
