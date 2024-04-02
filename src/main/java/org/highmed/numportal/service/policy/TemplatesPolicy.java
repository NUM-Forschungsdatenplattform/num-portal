package org.highmed.numportal.service.policy;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.highmed.numportal.service.exception.SystemException;

import java.util.List;
import java.util.Map;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_AQL;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NO_TEMPLATES_ATTACHED_TO_THE_PROJECT;

/** Restricts the aql to a set of templates defined by the project */
@Slf4j
public class TemplatesPolicy extends Policy {

  private static final String TEMPLATE_ID_PATH = "archetype_details/template_id/value";

  private Map<String, String> templatesMap;

  @Builder
  public TemplatesPolicy(Map<String, String> templatesMap) {
    this.templatesMap = templatesMap;
  }

  @Override
  public boolean apply(AqlQuery aql) {
    if (MapUtils.isEmpty(templatesMap)) {
      log.error(NO_TEMPLATES_ATTACHED_TO_THE_PROJECT);
      return true;
    }

    if (aql == null) {
      throw new SystemException(TemplatesPolicy.class, INVALID_AQL);
    }

    logAqlQuery(log, aql,"[AQL QUERY] Aql before executing TemplatesPolicy: %s ");

    List<Primitive> templateValues = toSimpleValueList(templatesMap.keySet());
    restrictAqlWithCompositionAttribute(aql, TEMPLATE_ID_PATH, templateValues);

    logAqlQuery(log, aql,"[AQL QUERY] Aql after executing TemplatesPolicy: %s ");
    return true;
  }
}
