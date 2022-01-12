package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.web.exception.ResourceNotFound;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AqlMapper {

  private final ModelMapper modelMapper;
  private final UserService userService;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public AqlDto convertToDto(Aql aql) {
    AqlDto aqlDto = modelMapper.map(aql, AqlDto.class);

    try {
      aqlDto.setOwner(userService.getOwner(aql.getOwner().getUserId()));
    } catch (ResourceNotFound e) {
      log.warn("Aql owner not found in keycloak: ", aql.getOwner().getUserId());
      aqlDto.setOwner(null);
    }
    return aqlDto;
  }

  public Aql convertToEntity(AqlDto aqlDto) {
    Aql aql = modelMapper.map(aqlDto, Aql.class);
    aql.setId(null);
    return aql;
  }
}
