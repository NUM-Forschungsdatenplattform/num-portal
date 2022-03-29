package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.properties.PrivacyProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PseudonymityTest {

  @Mock
  private PrivacyProperties privacyProperties;

  @Mock
  private EhrBaseService ehrBaseService;

  @InjectMocks
  private Pseudonymity pseudonymity;
}
