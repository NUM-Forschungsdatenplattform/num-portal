package org.highmed.numportal.service.executors;

import org.highmed.numportal.domain.model.CohortAql;
import org.highmed.numportal.properties.ConsentProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.generator.commons.aql.parameter.ParameterValue;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.policy.EuropeanConsentPolicy;
import org.highmed.numportal.service.policy.ProjectPolicyService;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class AqlExecutor {

  private final EhrBaseService ehrBaseService;

  private final ProjectPolicyService projectPolicyService;

  private final ConsentProperties consentProperties;

  public Set<String> execute(
      CohortAql aql, Map<String, Object> parameters, Boolean allowUsageOutsideEu) {

    if (aql != null && StringUtils.isNotEmpty(aql.getQuery())) {
      if (BooleanUtils.isTrue(allowUsageOutsideEu) || allowUsageOutsideEu == null) {
        applyPolicy(aql);
      }

      String query = removeNullParameters(parameters, aql.getQuery());
      query = addParameters(parameters, query);

      return ehrBaseService.retrieveEligiblePatientIds(query);
    } else {
      return SetUtils.emptySet();
    }
  }

  private void applyPolicy(CohortAql cohortAql) {
    AqlQuery aql = AqlQueryParser.parse(cohortAql.getQuery());
    projectPolicyService.apply(
        aql,
        List.of(
            EuropeanConsentPolicy.builder()
                .oid(consentProperties.getAllowUsageOutsideEuOid())
                .build()));

    cohortAql.setQuery(AqlRenderer.render(aql));
  }

  private String addParameters(Map<String, Object> parameters, String query) {
    if (MapUtils.isNotEmpty(parameters) && StringUtils.isNotEmpty(query)) {
      for (var v : getParameterValues(parameters)) {
        query = query.replace(v.getParameter().getAqlParameter(), v.buildAql());
      }
    }
    return query;
  }

  private String removeNullParameters(Map<String, Object> parameters, String query) {
    if (MapUtils.isNotEmpty(parameters) && StringUtils.isNotEmpty(query)) {

      Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        if (entry.getValue() == null) {
          query = AqlUtil.removeParameter(query, entry.getKey());
          iterator.remove();
        }
      }
    }
    return query;
  }

  private List<ParameterValue> getParameterValues(Map<String, Object> parameters) {
    List<ParameterValue> parameterValues = new LinkedList<>();
    parameters.forEach((k, v) -> parameterValues.add(new ParameterValue(k, v)));
    return parameterValues;
  }
}
