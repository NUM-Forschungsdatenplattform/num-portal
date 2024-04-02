package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.TemplateMetadataDto;
import org.highmed.numportal.mapper.TemplateMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aqleditor.dto.containment.ContainmentDto;
import org.ehrbase.aqleditor.service.AqlEditorContainmentService;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectClause;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.util.AqlQueryConstants;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_FIND_TEMPLATE;

@Service
@AllArgsConstructor
public class TemplateService {

  private final EhrBaseService ehrBaseService;

  private final TemplateMapper templateMapper;

  private final UserDetailsService userDetailsService;

  private final AqlEditorContainmentService aqlEditorContainmentService;

  /**
   * Retrieves a list of all available templates metadata information
   *
   * @return getAllTemplatesMetadata(String userId)
   */
  public List<TemplateMetadataDto> getAllTemplatesMetadata(String userId) {
    userDetailsService.checkIsUserApproved(userId);

    List<TemplateMetaDataDto> templateMetaDataDtos = ehrBaseService.getAllTemplatesMetadata();
    return templateMetaDataDtos.stream()
        .map(templateMapper::convertToTemplateMetadataDto)
        .collect(Collectors.toList());
  }

  public AqlQuery createSelectCompositionQuery(String templateId) {

    try {
      ContainmentDto containmentDto = aqlEditorContainmentService.buildContainment(templateId);
      if (containmentDto != null && StringUtils.isNotEmpty(containmentDto.getArchetypeId())) {
        return createQuery(containmentDto.getArchetypeId());
      } else {
        throw new BadRequestException(TemplateService.class, CANNOT_FIND_TEMPLATE, String.format(CANNOT_FIND_TEMPLATE, templateId));
      }
    } catch (SystemException e) {
      throw new SystemException(TemplateService.class, CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID,
              String.format(CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID, templateId));
    }
  }

  private AqlQuery createQuery(String archetypeId) {
    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.COMPOSITION_TYPE);
    containmentClassExpression.setIdentifier(AqlQueryConstants.COMPOSITION_CONTAINMENT_IDENTIFIER);
    IdentifiedPath identifiedPath = new IdentifiedPath();
    identifiedPath.setRoot(containmentClassExpression);

    // generate select expression
    SelectClause selectClause = new SelectClause();
    SelectExpression se = new SelectExpression();
    se.setColumnExpression(identifiedPath);
    se.setAlias("F1");
    selectClause.setStatement(List.of(se));

    // generate from expression
    ContainmentClassExpression from = new ContainmentClassExpression();
    from.setType(AqlQueryConstants.EHR_TYPE);
    from.setIdentifier(AqlQueryConstants.EHR_CONTAINMENT_IDENTIFIER);

    ContainmentClassExpression contains = new ContainmentClassExpression();
    contains.setType(AqlQueryConstants.COMPOSITION_TYPE);
    contains.setIdentifier(AqlQueryConstants.COMPOSITION_CONTAINMENT_IDENTIFIER);

    List<AndOperatorPredicate> fromPredList = new ArrayList<>();
    ComparisonOperatorPredicate fromPred = new ComparisonOperatorPredicate(AqlObjectPathUtil.ARCHETYPE_NODE_ID,
            ComparisonOperatorPredicate.PredicateComparisonOperator.EQ, new StringPrimitive(archetypeId));
    fromPredList.add(new AndOperatorPredicate(List.of(fromPred)));
    contains.setPredicates(fromPredList);
    from.setContains(contains);

    AqlQuery aqlQuery = new AqlQuery();
    aqlQuery.setSelect(selectClause);
    aqlQuery.setFrom(from);

    return aqlQuery;
  }
}
