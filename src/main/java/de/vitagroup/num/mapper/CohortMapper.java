/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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

    PropertyMap<CohortGroup, CohortGroupDto> cohortGroupDtoMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setPhenotypeId(source.getPhenotype().getId());
          }
        };

    PropertyMap<Cohort, CohortDto> cohortDtoMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setStudyId(source.getStudy().getId());
          }
        };

    modelMapper.addMappings(cohortDtoMap);
    modelMapper.addMappings(cohortGroupDtoMap);
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
