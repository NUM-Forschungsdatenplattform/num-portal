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
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class TemplateMapperTest {

  @Spy private ModelMapper modelMapper;

  @InjectMocks private TemplateMapper templateMapper;

  @Before
  public void setup() {
    templateMapper.initialize();
  }

  @Test
  public void shouldCorrectlyConvertToTemplateMetadataDto() {
    TemplateMetaDataDto ehrMetaDataDto = new TemplateMetaDataDto();

    ehrMetaDataDto.setTemplateId("ehrTemplateId");
    ehrMetaDataDto.setConcept("Concept");
    ehrMetaDataDto.setArchetypeId("ArchetypeId");
    ehrMetaDataDto.setCreatedOn(OffsetDateTime.now());

    TemplateMetadataDto numDto = templateMapper.convertToTemplateMetadataDto(ehrMetaDataDto);

    assertThat(numDto, notNullValue());
    assertThat(numDto.getTemplateId(), is(ehrMetaDataDto.getTemplateId()));
    assertThat(numDto.getCreatedOn(), is(ehrMetaDataDto.getCreatedOn()));
    assertThat(numDto.getArchetypeId(), is(ehrMetaDataDto.getArchetypeId()));
    assertThat(numDto.getName(), is(ehrMetaDataDto.getConcept()));
  }

  @Test
  public void shouldCorrectlyConvertToTemplateDtoList() {
    Map<String, String> templates = new HashMap<>();
    templates.put("t1", "v1");
    templates.put("t2", "v1");

    List<TemplateInfoDto> dtos = templateMapper.convertToTemplateInfoDtoList(templates);

    assertThat(dtos, notNullValue());
    assertThat(
        dtos.stream().anyMatch(templateInfoDto -> templateInfoDto.getTemplateId() == "t1"),
        is(true));
    assertThat(
        dtos.stream().anyMatch(templateInfoDto -> templateInfoDto.getTemplateId() == "t2"),
        is(true));
  }

  @Test
  public void shouldHandleNullMap() {
    List<TemplateInfoDto> dtos = templateMapper.convertToTemplateInfoDtoList(null);

    assertThat(dtos, notNullValue());
    assertThat(dtos.size(), is(0));
  }
}
