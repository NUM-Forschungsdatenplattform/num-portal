package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import lombok.AllArgsConstructor;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class TemplateMapper {

  private final ModelMapper modelMapper;

  public TemplateMetadataDto convertToTemplateMetadataDto(TemplateMetaDataDto metaDataDto) {
    return modelMapper.map(metaDataDto, TemplateMetadataDto.class);
  }

  public List<TemplateInfoDto> convertToTemplateInfoDtoList(Map<String, String> templateInfoMap) {
    return templateInfoMap.entrySet().stream()
        .map(e -> TemplateInfoDto.builder().id(e.getKey()).concept(e.getValue()).build())
        .collect(Collectors.toList());
  }
}
