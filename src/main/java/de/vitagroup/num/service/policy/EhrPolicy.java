package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_SIZE_CANNOT_BE_0;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

/** Restricts the aql to a set of ehr ids defined by the project cohort */
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
    //select.setContainmentId(aql.getEhr().getContainmentId());

    extendWhereClause(aql, List.of(select), toSimpleValueList(new ArrayList<>(cohortEhrIds)));
    return true;
  }
}
