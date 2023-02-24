package de.vitagroup.num.service.ehrbase;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.XmlParser;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.properties.PseudonymsPsnWorkflowProperties;
import de.vitagroup.num.service.exception.ResourceNotFound;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
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

    private static final String RESPONSE_BODY_WITH_ERROR = "<Parameters xmlns=\"http://hl7.org/fhir\"><parameter><name value=\"error\"/><part><name value=\"all\"/><valueString value=\"NULL\"/></part><part><name value=\"error-code\"/><valueCoding><system value=\"https://www.hl7.org/fhir/valueset-issue-type.html\"/><code value=\"exception\"/><display value=\"Exception\"/></valueCoding></part></parameter></Parameters>";

    private static final String RESPONSE_BODY_BAD_REQUEST_ERROR = "<OperationOutcome xmlns=\"http://hl7.org/fhir\"><issue><severity value=\"error\"/><code value=\"processing\"/><diagnostics value=\"Missing study parameter\"/></issue></OperationOutcome>";

    private CloseableHttpResponse response;

    private IParser xmlParser;

  @Test
  public void getPseudonyms() throws IOException {
      when(xmlParser.encodeResourceToString(Mockito.any(Parameters.class))).thenReturn(REQUEST_BODY);
      when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
      StringEntity entity = new StringEntity(RESPONSE_BODY, ContentType.parse("application/fhir+xml;charset=utf-8"));
      when(response.getEntity()).thenReturn(entity);
      when(xmlParser.parseResource(Mockito.any(), Mockito.any(String.class))).thenReturn(mockOkParameters());
      when(closeableHttpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
      pseudonymity.getPseudonyms(Arrays.asList("codex-AB1234"), 100L);
  }

    @Test(expected = ResourceNotFound.class)
    public void getPseudonymsNotFound() throws IOException {
        when(xmlParser.encodeResourceToString(Mockito.any(Parameters.class))).thenReturn(REQUEST_BODY);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
        StringEntity entity = new StringEntity(RESPONSE_BODY_WITH_ERROR, ContentType.parse("application/fhir+xml;charset=utf-8"));
        when(response.getEntity()).thenReturn(entity);
        when(xmlParser.parseResource(Mockito.any(), Mockito.any(String.class))).thenReturn(mockErrorParameters());
        when(closeableHttpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
        pseudonymity.getPseudonyms(Arrays.asList("codex-AB1234"), 100L);
    }

    @Test(expected = ResourceNotFound.class)
    public void getPseudonymsMissingParamsBadRequest() throws IOException {
        when(xmlParser.encodeResourceToString(Mockito.any(Parameters.class))).thenReturn(REQUEST_BODY);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Missing params"));
        StringEntity entity = new StringEntity(RESPONSE_BODY_BAD_REQUEST_ERROR, ContentType.parse("application/fhir+xml;charset=utf-8"));
        when(response.getEntity()).thenReturn(entity);
        when(closeableHttpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
        pseudonymity.getPseudonyms(Arrays.asList("codex-AB1234"), 100L);
    }

    @Before
    public void setup() {
        response = Mockito.mock(CloseableHttpResponse.class);
        when(fttpProperties.getUrl()).thenReturn("http://url.com");
        xmlParser = Mockito.mock(XmlParser.class);
        when(fhirContext.newXmlParser()).thenReturn(xmlParser);
    }

    private Parameters mockErrorParameters() {
      Parameters parameters = new Parameters();
      parameters.addParameter("error", "NULL");
      parameters.addParameter("error-code", "exception");
      return parameters;
    }

    private Parameters mockOkParameters() {
        Parameters parameters = new Parameters();
        Parameters.ParametersParameterComponent parametersParameterComponent = new Parameters.ParametersParameterComponent(new StringType("pseudonym"));
        Parameters.ParametersParameterComponent original = mockParamComponent("original", "codex_WX6QAM");
        parametersParameterComponent.addPart(original);
        Parameters.ParametersParameterComponent pseudo = mockParamComponent("pseudonym", "extern_0_E2F4D9");
        parametersParameterComponent.addPart(pseudo);

        parameters.addParameter(parametersParameterComponent);
        return parameters;
    }

    private Parameters.ParametersParameterComponent mockParamComponent(String name, String value){
        Parameters.ParametersParameterComponent component = new Parameters.ParametersParameterComponent(new StringType(name));
        Identifier identifier = new Identifier();
        identifier.setSystem("some system");
        identifier.setValue(value);
        component.setValue(identifier);
        return component;
    }
}
