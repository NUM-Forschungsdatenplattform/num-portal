package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;

import java.util.List;
import java.util.Map;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.NO_TEMPLATES_ATTACHED_TO_THE_PROJECT;

/** Restricts the aql to a set of templates defined by the project */
@Slf4j
public class TemplatesPolicy extends Policy {

  private static final String TEMPLATE_ID_PATH = "/archetype_details/template_id/value";

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

    log.debug(
            String.format(
                    "[AQL QUERY] Aql before executing TemplatesPolicy: %s ",
                    AqlRenderer.render(aql)));
    List<Primitive> templateValues = toSimpleValueList(templatesMap.keySet());
    restrictAqlWithCompositionAttribute(aql, TEMPLATE_ID_PATH, templateValues);
    log.debug(
            String.format(
                    "[AQL QUERY] Aql after executing TemplatesPolicy: %s ",
                    AqlRenderer.render(aql)));
    return true;
  }
}
