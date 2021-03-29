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

import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import lombok.AllArgsConstructor;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class TemplateMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {
    PropertyMap<TemplateMetaDataDto, TemplateMetadataDto> studyPropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setName(source.getConcept());
          }
        };

    modelMapper.addMappings(studyPropertyMap);
  }

  public TemplateMetadataDto convertToTemplateMetadataDto(TemplateMetaDataDto metaDataDto) {
    return modelMapper.map(metaDataDto, TemplateMetadataDto.class);
  }

  public List<TemplateInfoDto> convertToTemplateInfoDtoList(Map<String, String> templateInfoMap) {
    if (templateInfoMap != null) {
      return templateInfoMap.entrySet().stream()
          .map(e -> TemplateInfoDto.builder().templateId(e.getKey()).name(e.getValue()).build())
          .collect(Collectors.toList());
    } else {
      return List.of();
    }
  }
}
