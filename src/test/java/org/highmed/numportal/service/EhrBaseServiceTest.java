package org.highmed.numportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.generator.commons.aql.query.NativeQuery;
import org.ehrbase.openehr.sdk.generator.commons.aql.query.Query;
import org.ehrbase.openehr.sdk.generator.commons.aql.record.Record;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.TemplatesResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.ehrbase.openehr.sdk.util.exception.WrongStatusCodeException;
import org.highmed.numportal.properties.EhrBaseProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.numportal.domain.model.Aql;
import org.highmed.numportal.service.ehrbase.CompositionResponseDataBuilder;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.ehrbase.Pseudonymity;
import org.highmed.numportal.service.exception.SystemException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EhrBaseServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DefaultRestClient restClient;

  @Mock
  public ObjectMapper mapper;

  @Mock
  public CompositionResponseDataBuilder compositionResponseDataBuilder;

  @Mock
  private Pseudonymity pseudonymity;

  @Mock
  private EhrBaseProperties ehrBaseProperties;

  @InjectMocks
  private EhrBaseService ehr;

  private static final String GOOD_QUERY =
          "Select c0 as test from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";

  @Test(expected = WrongStatusCodeException.class)
  public void shouldHandleBadAqlQuery() {
    when(restClient.aqlEndpoint().execute(any(Query.class)))
            .thenThrow(WrongStatusCodeException.class);
    ehr.retrieveEligiblePatientIds(Aql.builder().query("SELECT e/ehr_id/value FROM EHR e").build());
  }

  @Test(expected = AqlParseException.class)
  public void shouldHandleMalformedAqlQuery() {
    when(restClient.aqlEndpoint().execute(any(Query.class)))
            .thenThrow(WrongStatusCodeException.class);
    ehr.retrieveEligiblePatientIds(Aql.builder().query("SLECT e/ehr_id/value FROM EHR e").build());
  }


  @Test
  public void shouldReplaceSelect() {
    when(restClient.aqlEndpoint().execute(any(Query.class))).thenReturn(Collections.emptyList());
    ehr.retrieveEligiblePatientIds(Aql.builder().query("SELECT e/ehr_id FROM EHR e").build());
    ArgumentCaptor<NativeQuery<Record>> queryArgumentCaptor =
            ArgumentCaptor.forClass(NativeQuery.class);
    verify(restClient.aqlEndpoint(), times(1)).execute(queryArgumentCaptor.capture());
    assertThat(queryArgumentCaptor.getValue().buildAql(), is("SELECT e/ehr_id/value FROM EHR e"));
  }

  @Test
  public void shouldRetrieveAllTemplates() {
    List<TemplateMetaDataDto> templates = ehr.getAllTemplatesMetadata();

    assertThat(templates, notNullValue());
    assertThat(templates.size(), is(2));
    assertThat(templates.get(0).getTemplateId(), is("t1"));
    assertThat(templates.get(0).getConcept(), is("c1"));
    assertThat(templates.get(1).getTemplateId(), is("t2"));
    assertThat(templates.get(1).getConcept(), is("c2"));
  }

  @Test(expected = SystemException.class)
  public void shouldHandleExceptionWhenRetrieveAllTemplates() {
    when(restClient.templateEndpoint()).thenThrow(ClientException.class);
    ehr.getAllTemplatesMetadata();
  }

  @Test
  public void shouldFlattenResultsWhenContainsComposition() {
    QueryResponseData compositionsQueryResponseData = new QueryResponseData();
    List<Map<String, String>> columns =
            new ArrayList<>(List.of(Map.of("path", "/ehr_status/subject/external_ref/id/value"), Map.of("uuid", "c/uuid")));
    List<List<Object>> rows =
            List.of(
                    new ArrayList<>(List.of("testehrId", Map.of("_type", "COMPOSITION", "uuid", "12345"))),
                    new ArrayList<>(List.of("testehrId2", Map.of("_type", "COMPOSITION", "uuid", "bla"))));
    compositionsQueryResponseData.setColumns(columns);
    compositionsQueryResponseData.setRows(rows);

    when(ehrBaseProperties.getIdPath()).thenReturn("ehr_status/subject/external_ref/id/value");
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenReturn(compositionsQueryResponseData);

    when(compositionResponseDataBuilder.build(any())).thenReturn(compositionsQueryResponseData);

    ehr.executeRawQuery(AqlQueryParser.parse(GOOD_QUERY), 1L);
    verify(compositionResponseDataBuilder, times(1)).build(any());
  }

  @Test
  public void shouldNotFlattenResults() {
    QueryResponseData response = new QueryResponseData();

    response.setColumns(
            new ArrayList<>(List.of(Map.of("path", "/ehr_status/subject/external_ref/id/value"), Map.of("uuid", "c/uuid"))));
    response.setRows(List.of(
            new ArrayList<>(List.of("testehrid1", Map.of("_type", "OBSERVATION", "uuid", "12345"))),
            new ArrayList<>(List.of("testehrid2", Map.of("_type", "SECTION", "uuid", "bla")))));

    when(ehrBaseProperties.getIdPath()).thenReturn("ehr_status/subject/external_ref/id/value");
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenReturn(response);

    ehr.executeRawQuery(AqlQueryParser.parse(GOOD_QUERY), 1L);
    verify(compositionResponseDataBuilder, times(0)).build(any());
  }

  @Test(expected = SystemException.class)
  public void shouldHandleClientExceptionWhenExecutingAql() {
    when(ehrBaseProperties.getIdPath()).thenReturn("path/to/config");
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenThrow(ClientException.class);
    ehr.executeRawQuery(AqlQueryParser.parse(GOOD_QUERY), 1L);
  }
  @Test(expected = SystemException.class)
  public void shouldHandleClientExceptionWhenConnectToEhrBase() {
    when(restClient.aqlEndpoint().execute(any(Query.class)))
            .thenThrow(ClientException.class);
    ehr.getAllPatientIds();
  }

  @Test(expected = WrongStatusCodeException.class)
  public void shouldHandleWrongStatusCodeExceptionWhenExecutingAql(){
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenThrow(WrongStatusCodeException.class);
    ehr.executeRawQuery(AqlQueryParser.parse(GOOD_QUERY), 1L);
  }

  @Test(expected = WrongStatusCodeException.class)
  public void shouldHandleWrongStatusCodeExceptionWhenExecutePlainQuery(){
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenThrow(WrongStatusCodeException.class);
    ehr.executePlainQuery(GOOD_QUERY);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleClientExceptionWhenExecutePlainQuery() {
    when(restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(any())))
            .thenThrow(ClientException.class);
    ehr.executePlainQuery(GOOD_QUERY);
  }

  @Before
  public void setup() {
    TemplatesResponseData templatesResponseData = new TemplatesResponseData();

    TemplateMetaDataDto t1 = new TemplateMetaDataDto();
    t1.setTemplateId("t1");
    t1.setConcept("c1");

    TemplateMetaDataDto t2 = new TemplateMetaDataDto();
    t2.setTemplateId("t2");
    t2.setConcept("c2");

    templatesResponseData.set(List.of(t1, t2));

    when(restClient.templateEndpoint().findAllTemplates()).thenReturn(templatesResponseData);
    when(pseudonymity.getPseudonyms(anyList(), anyLong())).thenReturn(List.of("codex_43DG23", "codex_43DG22"));
  }
}