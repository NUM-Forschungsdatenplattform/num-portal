package de.vitagroup.num.service.policy;

import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;

/** Restricts the aql to a set of ehr ids defined by the project cohort */
public class EhrPolicy extends Policy {

  private static final String EHR_ID_PATH = "/ehr_id/value";
  private static final String ERROR_MESSAGE = "Cohort size cannot be 0";

  private Set<String> cohortEhrIds;

  @Builder
  public EhrPolicy(Set<String> cohortEhrIds) {
    this.cohortEhrIds = cohortEhrIds;
  }

  @Override
  public void apply(AqlDto aql) {
    if (aql == null) {
      throw new SystemException(AQL_ERROR_MESSAGE);
    }

    if (CollectionUtils.isEmpty(cohortEhrIds)) {
      throw new SystemException(ERROR_MESSAGE);
    }

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(EHR_ID_PATH);
    select.setContainmentId(aql.getEhr().getContainmentId());

    extendWhereClause(aql, List.of(select), toSimpleValueList(cohortEhrIds));
  }
}
