package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.TemplateInfoDto;
import org.highmed.numportal.domain.dto.TemplateMetadataDto;

import lombok.AllArgsConstructor;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

@Component
@AllArgsConstructor
public class TemplateMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {
    PropertyMap<TemplateMetaDataDto, TemplateMetadataDto> projectPropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setName(source.getConcept());
          }
        };

    modelMapper.addMappings(projectPropertyMap);
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
