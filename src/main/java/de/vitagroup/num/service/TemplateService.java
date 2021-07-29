package de.vitagroup.num.service;

import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.mapper.TemplateMapper;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.EhrDto;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.dto.select.SelectStatementDto;
import org.ehrbase.aqleditor.dto.containment.ContainmentDto;
import org.ehrbase.aqleditor.service.AqlEditorContainmentService;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TemplateService {

  private final EhrBaseService ehrBaseService;

  private final TemplateMapper templateMapper;

  private final UserDetailsService userDetailsService;

  private final AqlEditorContainmentService aqlEditorContainmentService;

  private static final int COMPOSITION_CONTAINMENT_ID = 1;
  private static final int EHR_CONTAINMENT_ID = 0;
  private static final String EHR_CONTAINMENT_IDENTIFIER = "e";

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

  public AqlDto createSelectCompositionQuery(String templateId) {

    try {
      ContainmentDto containmentDto = aqlEditorContainmentService.buildContainment(templateId);
      if (containmentDto != null && StringUtils.isNotEmpty(containmentDto.getArchetypeId())) {
        return createQuery(containmentDto.getArchetypeId());
      } else {
        throw new BadRequestException("Cannot find template: " + templateId);
      }
    } catch (Exception e) {
      throw new SystemException("Cannot create query for template " + templateId);
    }
  }

  private AqlDto createQuery(String archetypeId) {
    org.ehrbase.aql.dto.containment.ContainmentDto contains =
        new org.ehrbase.aql.dto.containment.ContainmentDto();
    contains.setArchetypeId(archetypeId);
    contains.setId(COMPOSITION_CONTAINMENT_ID);

    SelectFieldDto fieldDto = new SelectFieldDto();
    fieldDto.setContainmentId(COMPOSITION_CONTAINMENT_ID);
    fieldDto.setAqlPath(Strings.EMPTY);

    SelectDto select = new SelectDto();
    List<SelectStatementDto> fieldDtos = new LinkedList<>();
    fieldDtos.add(fieldDto);

    select.setStatement(fieldDtos);

    EhrDto ehrDto = new EhrDto();
    ehrDto.setContainmentId(EHR_CONTAINMENT_ID);
    ehrDto.setIdentifier(EHR_CONTAINMENT_IDENTIFIER);

    AqlDto dto = new AqlDto();
    dto.setEhr(ehrDto);
    dto.setSelect(select);
    dto.setContains(contains);

    return dto;
  }
}
