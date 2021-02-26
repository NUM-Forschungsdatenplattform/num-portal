package de.vitagroup.num.service;

import de.vitagroup.num.mapper.TemplateMapper;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import lombok.AllArgsConstructor;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TemplateService {

  private final EhrBaseService ehrBaseService;

  private final TemplateMapper templateMapper;

  /**
   * Retrieves a list of all available templates metadata information
   *
   * @return
   */
  public List<TemplateMetadataDto> getAllTemplatesMetadata() {
    List<TemplateMetaDataDto> templateMetaDataDtos = ehrBaseService.getAllTemplatesMetadata();
    return templateMetaDataDtos.stream()
        .map(templateMapper::convertToTemplateMetadataDto)
        .collect(Collectors.toList());
  }

}
