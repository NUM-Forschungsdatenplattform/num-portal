package de.vitagroup.num.service.ehrbase;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.XmlParser;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.properties.PseudonymsPsnWorkflowProperties;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PseudonymityTest {

  @Mock
  private PseudonymsPsnWorkflowProperties pseudonymsPsnWorkflowProperties;

  @Mock
  private FttpProperties fttpProperties;

  @Mock
  private FhirContext fhirContext;

  @Mock
  private CloseableHttpClient closeableHttpClient;

  @InjectMocks
  private Pseudonymity pseudonymity;

  private static final String REQUEST_BODY = "<Parameters xmlns=\"http://hl7.org/fhir\"><parameter><name value=\"study\"/><valueString value=\"num\"/></parameter><parameter><name value=\"source\"/><valueString value=\"codex\"/></parameter><parameter><name value=\"target\"/><valueString value=\"extern_0\"/></parameter><parameter><name value=\"apikey\"/><valueString value=\"iCZdh7ZWuf8ms)vvBgU-IaLi4\"/></parameter><parameter><name value=\"event\"/><valueString value=\"num.get_extern_psn\"/></parameter><parameter><name value=\"original\"/><valueString value=\"codex_WX6QAM\"/></parameter></Parameters>";

  private static final String RESPONSE_BODY = "<Parameters xmlns=\"http://hl7.org/fhir\"><parameter><name value=\"pseudonym\"/><part><name value=\"original\"/><valueIdentifier><system value=\"https://ths-greifswald.de/dispatcher\"/><value value=\"codex_NC2PG0\"/></valueIdentifier></part><part><name value=\"target\"/><valueIdentifier><system value=\"https://ths-greifswald.de/dispatcher\"/><value value=\"extern_0\"/></valueIdentifier></part><part><name value=\"pseudonym\"/><valueIdentifier><system value=\"https://ths-greifswald.de/dispatcher\"/><value value=\"extern_0_E2F4D9\"/></valueIdentifier></part></parameter></Parameters>";
  @Test
  public void getPseudonyms() throws IOException {
      CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
      when(fttpProperties.getUrl()).thenReturn("http://url.com");
      IParser xmlParser = Mockito.mock(XmlParser.class);
      when(fhirContext.newXmlParser()).thenReturn(xmlParser);
      when(xmlParser.encodeResourceToString(Mockito.any(Parameters.class))).thenReturn(REQUEST_BODY);
      when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
      StringEntity entity = new StringEntity(RESPONSE_BODY, ContentType.parse("application/fhir+xml;charset=utf-8"));
      when(response.getEntity()).thenReturn(entity);
      when(xmlParser.parseResource(Mockito.any(), Mockito.any(String.class))).thenReturn(new Parameters());
      when(closeableHttpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
      pseudonymity.getPseudonyms(Arrays.asList("codex-AB1234"), 100L);
  }
}
