package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CohortMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {
    PropertyMap<Cohort, CohortDto> cohortDtoMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setProjectId(source.getProject().getId());
          }
        };

    modelMapper.addMappings(cohortDtoMap);
  }

  public CohortDto convertToDto(Cohort cohort) {
    CohortDto cohortDto = modelMapper.map(cohort, CohortDto.class);
    CohortGroupDto cohortGroupDto = convertToCohortGroupDto(cohort.getCohortGroup());
    cohortDto.setCohortGroup(cohortGroupDto);
    return cohortDto;
  }

  private CohortGroupDto convertToCohortGroupDto(CohortGroup cohortGroup) {
    CohortGroupDto dto = modelMapper.map(cohortGroup, CohortGroupDto.class);
    if (cohortGroup.getType().equals(Type.GROUP)) {
      dto.setChildren(
          cohortGroup.getChildren().stream()
              .map(this::convertToCohortGroupDto)
              .collect(Collectors.toList()));
    }
    return dto;
  }
}
