package de.vitagroup.num.service;

import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.mapper.TemplateMapper;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TemplateService {

  private final EhrBaseService ehrBaseService;

  private final TemplateMapper templateMapper;

  private final UserDetailsService userDetailsService;

  /**
   * Retrieves a list of all available templates metadata information
   *
   * @return
   */
  public List<TemplateMetadataDto> getAllTemplatesMetadata(String userId) {
    userDetailsService.checkIsUserApproved(userId);

    List<TemplateMetaDataDto> templateMetaDataDtos = ehrBaseService.getAllTemplatesMetadata();
    return templateMetaDataDtos.stream()
        .map(templateMapper::convertToTemplateMetadataDto)
        .collect(Collectors.toList());
  }
}
