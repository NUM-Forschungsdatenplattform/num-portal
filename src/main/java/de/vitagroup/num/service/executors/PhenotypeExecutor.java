package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.properties.ConsentProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import de.vitagroup.num.service.policy.EuropeanConsentPolicy;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.client.aql.parameter.ParameterValue;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PhenotypeExecutor {

  private final SetOperationsService setOperations;

  private final EhrBaseService ehrBaseService;

  private final ProjectPolicyService projectPolicyService;

  private final ConsentProperties consentProperties;

  public Set<String> execute(Phenotype phenotype, Map<String, Object> parameters) {
    if (phenotype == null || phenotype.getQuery() == null) {
      throw new IllegalArgumentException("Cannot execute an empty phenotype");
    }
    Set<String> all = ehrBaseService.getAllPatientIds();
    return execute(phenotype.getQuery(), all, parameters);
  }

  public Set<String> execute(Phenotype phenotype) {

    if (phenotype == null || phenotype.getQuery() == null) {
      throw new IllegalArgumentException("Cannot execute an empty phenotype");
    }
    Set<String> all = ehrBaseService.getAllPatientIds();
    return execute(phenotype.getQuery(), all, Map.of());
  }

  private Set<String> execute(
      Expression expression, Set<String> all, Map<String, Object> parameters) {
    if (expression instanceof GroupExpression) {
      GroupExpression groupExpression = (GroupExpression) expression;
      List<Set<String>> sets =
          groupExpression.getChildren().stream()
              .map(e -> execute(e, all, parameters))
              .collect(Collectors.toList());

      return setOperations.apply(groupExpression.getOperator(), sets, all);

    } else if (expression instanceof AqlExpression) {

      AqlExpression aqlExpression = (AqlExpression) expression;
      addParameters(parameters, aqlExpression);

      Aql aql = applyPolicy(aqlExpression.getAql());
      return ehrBaseService.retrieveEligiblePatientIds(aql);
    }
    return SetUtils.emptySet();
  }

  private Aql applyPolicy(Aql query) {
    AqlDto aql = new AqlToDtoParser().parse(query.getQuery());
    projectPolicyService.apply(
        aql, List.of(EuropeanConsentPolicy.builder().oid(consentProperties.getNonEuOid()).build()));

    String restrictedQuery = new AqlBinder().bind(aql).getLeft().buildAql();
    return Aql.builder().query(restrictedQuery).build();
  }

  private void addParameters(Map<String, Object> parameters, AqlExpression aqlExpression) {
    if (MapUtils.isNotEmpty(parameters)) {

      if (aqlExpression.getAql() == null) {
        return;
      }

      Aql aql = aqlExpression.getAql();
      String query = aql.getQuery();

      if (StringUtils.isEmpty(query)) {
        return;
      }

      for (ParameterValue v : getParameterValues(parameters)) {
        query = query.replace(v.getParameter().getAqlParameter(), v.buildAql());
      }
      aql.setQuery(query);
    }
  }

  private List<ParameterValue> getParameterValues(Map<String, Object> parameters) {
    List<ParameterValue> parameterValues = new LinkedList<>();
    parameters.forEach((k, v) -> parameterValues.add(new ParameterValue(k, v)));
    return parameterValues;
  }
}
