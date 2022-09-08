package de.vitagroup.num.service.policy;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_SIZE_CANNOT_BE_0;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;
import static org.mockito.Mockito.when;

import org.ehrbase.aql.dto.AqlDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.service.exception.SystemException;

@RunWith(MockitoJUnitRunner.class)
public class EuropeanConsentPolicyTest {

  @InjectMocks private EuropeanConsentPolicy europeanConsentPolicy;

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNull() {
    EuropeanConsentPolicy europeanConsentPolicy = EuropeanConsentPolicy.builder().build();
    AqlDto aqlDto = new AqlDto();
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
