package de.vitagroup.num.converter;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.UserDetails;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@AllArgsConstructor
public class UserDetailsConverter {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void setUp() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public UserDetailsDto convertToDto(UserDetails userDetails) {
    return modelMapper.map(userDetails, UserDetailsDto.class);
  }

  public UserDetails convertToEntity(UserDetailsDto dto) {
    return modelMapper.map(dto, UserDetails.class);
  }
}
