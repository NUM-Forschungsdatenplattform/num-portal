package de.vitagroup.num.service.ehrbase;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PseudonymityTest {

  @Spy private PrivacyProperties privacyProperties;

  @Mock private EhrBaseService ehrBaseService;

  @InjectMocks private Pseudonymity pseudonymity;

  @Test
  public void shouldPseudonymShouldBeReversible() {
    when(ehrBaseService.getAllPatientIds()).thenReturn(Set.of("notTesttestttest", "testtesttest"));
    String pseudonym = pseudonymity.getPseudonym("testtesttest", 100L);
    assertEquals("testtesttest", pseudonymity.getEhrIdFromPseudonym(pseudonym, 100L));
  }

  @Test
  public void shouldFailFindNonexistent() {
    when(ehrBaseService.getAllPatientIds())
        .thenReturn(Set.of("notTesttestttest", "Also Nottesttesttest"));
    String pseudonym = pseudonymity.getPseudonym("testtesttest", 100L);
    assertThrows(ResourceNotFound.class, () -> pseudonymity.getEhrIdFromPseudonym(pseudonym, 100L));
  }
}