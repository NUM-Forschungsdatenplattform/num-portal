package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.Value;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

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
  public void apply(AqlDto aql) {
    if (MapUtils.isEmpty(templatesMap)) {
      log.error("No templates attached to the project");
      return;
    }

    if (aql == null) {
      throw new SystemException(TemplatesPolicy.class, INVALID_AQL);
    }

    List<Value> templateValues = toSimpleValueList(templatesMap.keySet());
    restrictAqlWithCompositionAttribute(aql, TEMPLATE_ID_PATH, templateValues);
  }
}
