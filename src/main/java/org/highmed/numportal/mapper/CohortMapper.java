package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.CohortDto;
import org.highmed.numportal.domain.dto.CohortGroupDto;
import org.highmed.numportal.domain.model.Cohort;
import org.highmed.numportal.domain.model.CohortGroup;
import org.highmed.numportal.domain.model.Type;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;

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
