package de.vitagroup.num.service.policy;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_SIZE_CANNOT_BE_0;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;
import static org.mockito.Mockito.when;

import org.ehrbase.aql.dto.AqlDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.policy.EhrPolicy;

@RunWith(MockitoJUnitRunner.class)
public class EhrPolicyTest {

  @InjectMocks private EhrPolicy ehrPolicy;

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNull() {
    AqlDto aqlDto = new AqlDto();
    ehrPolicy.apply(aqlDto);
    when(ehrPolicy.apply(aqlDto))
            .thenThrow(new SystemException(EhrPolicy.class, COHORT_SIZE_CANNOT_BE_0));
    ehrPolicy.apply(aqlDto);
  }

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNotNullCohortEhrIdsAreEmpty() {
    ehrPolicy.apply(null);
    when(ehrPolicy.apply(null))
            .thenThrow(new SystemException(EhrPolicy.class, INVALID_AQL));
    ehrPolicy.apply(null);
  }
}
