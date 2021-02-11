package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import java.util.List;
import org.assertj.core.util.Lists;
import org.ehrbase.aql.parser.AqlParseException;
import org.ehrbase.client.aql.field.AqlFieldImp;
import org.ehrbase.client.aql.query.EntityQuery;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EhrBaseServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DefaultRestClient restClient;

  @InjectMocks private EhrBaseService ehr;

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
  }

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
    when(restClient.aqlEndpoint().execute(any(Query.class))).thenReturn(Lists.emptyList());
    ehr.retrieveEligiblePatientIds(Aql.builder().query("SELECT e/ehr_id FROM EHR e").build());
    ArgumentCaptor<EntityQuery<Record>> queryArgumentCaptor =
        ArgumentCaptor.forClass(EntityQuery.class);
    verify(restClient.aqlEndpoint(), times(1)).execute(queryArgumentCaptor.capture());
    AqlFieldImp fieldImp = (AqlFieldImp) queryArgumentCaptor.getValue().fields()[0];
    assertThat(fieldImp.getPath(), is("/ehr_id/value"));
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
}
