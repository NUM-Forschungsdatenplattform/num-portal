package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_SIZE_CANNOT_BE_0;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

/** Restricts the aql to a set of ehr ids defined by the project cohort */
@Slf4j
public class EhrPolicy extends Policy {

  private static final String EHR_ID_PATH = "/ehr_id/value";


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
    select.setPath(AqlObjectPath.parse(EHR_ID_PATH));

    SelectExpression se = new SelectExpression();
    se.setColumnExpression(select);

    log.debug(
            String.format(
                    "[AQL QUERY] Aql before executing EhrPolicy: %s ",
                    AqlRenderer.render(aql)));

    extendWhereClause(aql, List.of(se), toSimpleValueList(new ArrayList<>(cohortEhrIds)));

    log.debug(
            String.format(
                    "[AQL QUERY] Aql after executing EhrPolicy: %s ",
                    AqlRenderer.render(aql)));
    return true;
  }
}
