package de.vitagroup.num.service.policy;

import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.Value;

/**
 * Restricts the aql to a particular oid which defines the user consent for project data being used
 * outside the European Union
 */
@Slf4j
public class EuropeanConsentPolicy extends Policy {

  private static final String FEEDER_AUDIT_PATH =
      "/feeder_audit/feeder_system_audit/other_details/items[at0002]/value/id";

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
      throw new SystemException(AQL_ERROR_MESSAGE);
    }

    List<Value> oidValues = toSimpleValueList(List.of(oid));
    restrictAqlWithCompositionAttribute(aql, FEEDER_AUDIT_PATH, oidValues);
  }
}
