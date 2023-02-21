package de.vitagroup.num.service.ehrbase;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;

@RunWith(MockitoJUnitRunner.class)
public class PseudonymityTest {

  @Mock
  private PrivacyProperties privacyProperties;

  @Mock
  private EhrBaseService ehrBaseService;

  @InjectMocks
  private Pseudonymity pseudonymity;
  @Test(expected = SystemException.class)
  public void getPseudonymSecretIsNull() {
    when(privacyProperties.getPseudonymitySecret()).thenReturn(null);
    when(pseudonymity.getPseudonym("testtesttest", 100L))
            .thenThrow(new SystemException(Pseudonymity.class, PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED));
    pseudonymity.getPseudonym("testtesttest", 100L);
  }

  @Test(expected = ResourceNotFound.class)
  public void getEhrIdFromPseudonymGetAllPatientIdsIsEmpty() {
    when(pseudonymity.getEhrIdFromPseudonym("testtesttest", 100L))
            .thenThrow(new ResourceNotFound(Pseudonymity.class, EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND));
    pseudonymity.getEhrIdFromPseudonym("testtesttest", 100L);
  }

  @Before
}
