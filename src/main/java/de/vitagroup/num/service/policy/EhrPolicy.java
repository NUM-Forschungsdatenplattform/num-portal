package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.util.AqlQueryConstants;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_SIZE_CANNOT_BE_0;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

/** Restricts the aql to a set of ehr ids defined by the project cohort */
@Slf4j
public class EhrPolicy extends Policy {


  private Set<String> cohortEhrIds;

  @Builder
  public EhrPolicy(Set<String> cohortEhrIds) {
    this.cohortEhrIds = cohortEhrIds;
  }

  @Override
  public boolean apply(AqlQuery aql) {
    if (aql == null) {
      throw new SystemException(EhrPolicy.class, INVALID_AQL);
    }

    if (CollectionUtils.isEmpty(cohortEhrIds)) {
      throw new SystemException(EhrPolicy.class, COHORT_SIZE_CANNOT_BE_0);
    }

    IdentifiedPath select = new IdentifiedPath();
    select.setPath(AqlObjectPath.parse(AqlQueryConstants.EHR_ID_PATH));

    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.EHR_TYPE);
    containmentClassExpression.setIdentifier(AqlQueryConstants.EHR_CONTAINMENT_IDENTIFIER);
    select.setRoot(containmentClassExpression);

    SelectExpression se = new SelectExpression();
    se.setColumnExpression(select);

    logAqlQuery(log, aql, "[AQL QUERY] Aql before executing EhrPolicy: %s ");
    extendWhereClause(aql, List.of(se), toSimpleValueList(new ArrayList<>(cohortEhrIds)));
    logAqlQuery(log, aql, "[AQL QUERY] Aql after executing EhrPolicy: %s ");
    return true;
  }
}
