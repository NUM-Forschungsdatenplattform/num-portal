package org.highmed.numportal.service.policy;

import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.highmed.numportal.service.policy.EhrPolicy;
import org.highmed.numportal.service.policy.EuropeanConsentPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.numportal.service.exception.SystemException;

import static org.mockito.Mockito.when;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_AQL;

@RunWith(MockitoJUnitRunner.class)
public class EuropeanConsentPolicyTest {

  @InjectMocks private EuropeanConsentPolicy europeanConsentPolicy;

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNull() {
    EuropeanConsentPolicy europeanConsentPolicy = EuropeanConsentPolicy.builder().build();
    AqlQuery aqlDto = new AqlQuery();
    when(europeanConsentPolicy.apply(aqlDto))
            .thenThrow(new SystemException(EhrPolicy.class, CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED));
    europeanConsentPolicy.apply(aqlDto);
  }

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNotNullCohortEhrIdsAreEmpty() {
    EuropeanConsentPolicy europeanConsentPolicy = EuropeanConsentPolicy
            .builder()
            .oid("oid")
            .build();
    when(europeanConsentPolicy.apply(null))
            .thenThrow(new SystemException(EhrPolicy.class, INVALID_AQL));
    europeanConsentPolicy.apply(null);
  }
}
