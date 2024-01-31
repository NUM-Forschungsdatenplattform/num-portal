package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;

import java.util.List;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

/**
 * Restricts the aql to a particular oid which defines the user consent for project data being used
 * outside the European Union
 */
@Slf4j
public class EuropeanConsentPolicy extends Policy {

  private static final String FEEDER_AUDIT_PATH =
      "feeder_audit/feeder_system_audit/other_details[openEHR-EHR-ITEM_TREE.generic.v1]/items[at0001]/value/id";

  private String oid;

  @Builder
  public EuropeanConsentPolicy(String oid) {
    this.oid = oid;
  }

  @Override
  public boolean apply(AqlQuery aql) {
    if (oid == null) {
      throw new SystemException(EuropeanConsentPolicy.class, CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED);
    }

    if (aql == null) {
      throw new SystemException(EuropeanConsentPolicy.class, INVALID_AQL);
    }

    logAqlQuery(log, aql,"[AQL QUERY] Aql before executing EuropeanConsentPolicy: %s ");
    List<Primitive> oidValues = toSimpleValueList(List.of(oid));
    restrictAqlWithCompositionAttribute(aql, FEEDER_AUDIT_PATH, oidValues);

    logAqlQuery(log, aql,"[AQL QUERY] Aql after executing EuropeanConsentPolicy: %s ");
    return true;
  }
}
