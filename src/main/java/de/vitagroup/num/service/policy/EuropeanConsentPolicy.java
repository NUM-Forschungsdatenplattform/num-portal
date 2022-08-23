package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.exception.SystemException;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.Value;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;

/**
 * Restricts the aql to a particular oid which defines the user consent for project data being used
 * outside the European Union
 */
@Slf4j
public class EuropeanConsentPolicy extends Policy {

  private static final String FEEDER_AUDIT_PATH =
      "/feeder_audit/feeder_system_audit/other_details[openEHR-EHR-ITEM_TREE.generic.v1]/items[at0001]/value/id";

  private String oid;

  @Builder
  public EuropeanConsentPolicy(String oid) {
    this.oid = oid;
  }

  @Override
  public void apply(AqlDto aql) {
    if (oid == null) {
      log.error(
          "Cannot check consent for data usage outside the European Union, oid not configured");
      return;
    }

    if (aql == null) {
      throw new SystemException(EuropeanConsentPolicy.class, INVALID_AQL);
    }

    List<Value> oidValues = toSimpleValueList(List.of(oid));
    restrictAqlWithCompositionAttribute(aql, FEEDER_AUDIT_PATH, oidValues);
  }
}
