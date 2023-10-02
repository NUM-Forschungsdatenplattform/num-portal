package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.model.CohortAql;
import de.vitagroup.num.properties.ConsentProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.policy.EuropeanConsentPolicy;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.aql.util.AqlUtil;
import org.ehrbase.client.aql.parameter.ParameterValue;
import org.springframework.stereotype.Service;

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
    AqlDto aql = new AqlToDtoParser().parse(cohortAql.getQuery());
    projectPolicyService.apply(
        aql,
        List.of(
            EuropeanConsentPolicy.builder()
                .oid(consentProperties.getAllowUsageOutsideEuOid())
                .build()));

    cohortAql.setQuery(new AqlBinder().bind(aql).getLeft().buildAql());
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
