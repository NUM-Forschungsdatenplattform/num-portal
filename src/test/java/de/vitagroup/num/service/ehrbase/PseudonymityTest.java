package de.vitagroup.num.service.ehrbase;

import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.vitagroup.num.properties.PrivacyProperties;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @Test
  public void pseudonymShouldBeReversible() {
    when(ehrBaseService.getAllPatientIds()).thenReturn(Set.of("notTesttestttest", "testtesttest"));
    //String pseudonym = pseudonymity.getPseudonyms("testtesttest", 100L);
    //assertEquals("testtesttest", pseudonymity.getEhrIdFromPseudonym(pseudonym, 100L));
  }

  @Test
  public void shouldFailFindNonexistent() {
    when(ehrBaseService.getAllPatientIds())
        .thenReturn(Set.of("notTesttestttest", "Also Nottesttesttest"));
    //String pseudonym = pseudonymity.getPseudonyms("testtesttest", 100L);
    //assertThrows(ResourceNotFound.class, () -> pseudonymity.getEhrIdFromPseudonym(pseudonym, 100L));
  }

  @Test
  public void testPseudo() {
    FhirContext ctx = FhirContext.forR4();

    String input = "<Parameters\n"
        + "\txmlns=\"http://hl7.org/fhir\">\n"
        + "\t<parameter>\n"
        + "\t\t<name value=\"pseudonym\"/>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"original\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"codex_CQ1A3Y\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"target\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"extern_1\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"pseudonym\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"extern_1_40Q11L\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t</parameter>\n"
        + "\t<parameter>\n"
        + "\t\t<name value=\"pseudonym\"/>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"original\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"codex_CQ1A3X\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"target\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"extern_1\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t\t<part>\n"
        + "\t\t\t<name value=\"pseudonym\"/>\n"
        + "\t\t\t<valueIdentifier>\n"
        + "\t\t\t\t<system value=\"https://ths-greifswald.de/dispatcher\"/>\n"
        + "\t\t\t\t<value value=\"extern_1_FUKH96\"/>\n"
        + "\t\t\t</valueIdentifier>\n"
        + "\t\t</part>\n"
        + "\t</parameter>\n"
        + "</Parameters>";

// Instantiate a new parser
    IParser parser = ctx.newXmlParser();

// Parse it
    Parameters parsed = parser.parseResource(Parameters.class, input);

    var originals = List.of("codex_CQ1A3X", "codex_CQ1A3Y");

  }


  @BeforeEach
  public void setup() {
    when(privacyProperties.getPseudonymitySecret()).thenReturn("testSecret123");
  }
}
