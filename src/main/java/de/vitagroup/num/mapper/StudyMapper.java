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

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.service.UserService;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StudyMapper {

  private final ModelMapper modelMapper;
  private final TemplateMapper templateMapper;
  private final UserService userService;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    PropertyMap<Study, StudyDto> templatePropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setCohortId(source.getCohort().getId());
          }
        };

    modelMapper.addMappings(templatePropertyMap);
  }

  public StudyDto convertToDto(Study study) {
    StudyDto studyDto = modelMapper.map(study, StudyDto.class);
    studyDto.setTemplates(templateMapper.convertToTemplateInfoDtoList(study.getTemplates()));
    User coordinator = userService.getUserById(study.getCoordinator().getUserId(), false);
    studyDto.setCoordinator(coordinator);
    return studyDto;
  }
}
