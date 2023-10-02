package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.model.Aql;
import de.vitagroup.num.domain.dto.AqlCategoryDto;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

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
    if (Objects.nonNull(aql.getCategory())) {
      aqlDto.setCategory(modelMapper.map(aql.getCategory(), AqlCategoryDto.class));
    }
    aqlDto.setOwner(userService.getOwner(aql.getOwner().getUserId()));
    return aqlDto;
  }

  public Aql convertToEntity(AqlDto aqlDto) {
    Aql aql = modelMapper.map(aqlDto, Aql.class);
    aql.setId(null);
    return aql;
  }
}
