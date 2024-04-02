package org.highmed.numportal.mapper;

import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.highmed.numportal.mapper.TemplateMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.highmed.numportal.domain.dto.TemplateInfoDto;
import org.highmed.numportal.domain.dto.TemplateMetadataDto;

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
        dtos.stream().anyMatch(templateInfoDto -> "t1".equals(templateInfoDto.getTemplateId())),
        is(true));
    assertThat(
        dtos.stream().anyMatch(templateInfoDto -> "t2".equals(templateInfoDto.getTemplateId())),
        is(true));
  }

  @Test
  public void shouldHandleNullMap() {
    List<TemplateInfoDto> dtos = templateMapper.convertToTemplateInfoDtoList(null);

    assertThat(dtos, notNullValue());
    assertThat(dtos.size(), is(0));
  }
}
