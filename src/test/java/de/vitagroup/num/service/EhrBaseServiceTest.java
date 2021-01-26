package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.SystemException;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    ehr.retrieveEligiblePatientIds(Aql.builder().build());
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
