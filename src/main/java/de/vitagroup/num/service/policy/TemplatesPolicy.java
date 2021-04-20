package de.vitagroup.num.service.policy;

import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.apache.commons.collections.MapUtils;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.Value;

/** Restricts the aql to a set of templates defined by the project */
public class TemplatesPolicy extends Policy {

  private static final String TEMPLATE_ID_PATH = "/archetype_details/template_id/value";
  private static final String ERROR_MESSAGE = "No templates attached to this study";

  private Map<String, String> templatesMap;

  @Builder
  public TemplatesPolicy(Map<String, String> templatesMap) {
    this.templatesMap = templatesMap;
  }

  @Override
  public void apply(AqlDto aql) {
    if (aql == null) {
      throw new SystemException(AQL_ERROR_MESSAGE);
    }

    if (MapUtils.isEmpty(templatesMap)) {
      throw new SystemException(ERROR_MESSAGE);
    }

    List<Value> templateValues = toSimpleValueList(templatesMap.keySet());
    restrictAqlWithCompositionAttribute(aql, TEMPLATE_ID_PATH, templateValues);
  }
}
