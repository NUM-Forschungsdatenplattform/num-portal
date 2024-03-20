package org.highmed.service;

import org.ehrbase.aqleditor.dto.containment.ContainmentDto;
import org.ehrbase.aqleditor.service.AqlEditorContainmentService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.domain.dto.TemplateMetadataDto;
import org.highmed.domain.model.admin.UserDetails;
import org.highmed.mapper.TemplateMapper;
import org.highmed.service.ehrbase.EhrBaseService;
import org.highmed.service.exception.BadRequestException;
import org.highmed.service.exception.SystemException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.highmed.domain.templates.ExceptionsTemplate.CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID;
import static org.highmed.domain.templates.ExceptionsTemplate.CANNOT_FIND_TEMPLATE;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceTest {

  @Mock private EhrBaseService ehrBaseService;

  @Mock private TemplateMapper templateMapper;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private TemplateService templateService;

  @Mock private AqlEditorContainmentService aqlEditorContainmentService;

  @Before
  public void setup() {
    TemplateMetaDataDto t1 = new TemplateMetaDataDto();
    t1.setTemplateId("t1");
    t1.setConcept("c1");
    t1.setArchetypeId("a1");
    t1.setCreatedOn(OffsetDateTime.now());

    when(ehrBaseService.getAllTemplatesMetadata()).thenReturn(List.of(t1));
    when(templateMapper.convertToTemplateMetadataDto(any()))
        .thenReturn(TemplateMetadataDto.builder().name("t1").build());

    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    when(userDetailsService.checkIsUserApproved("approvedUserId"))
        .thenReturn(approvedUser);
  }

  @Test
  public void shouldCorrectlyRetrieveTemplateMetadata() {
    List<TemplateMetadataDto> numTemplates =
        templateService.getAllTemplatesMetadata("approvedUserId");

    assertThat(numTemplates, notNullValue());
    assertThat(numTemplates.size(), is(1));
    assertThat(numTemplates.get(0).getName(), is("t1"));
  }

  @Test(expected = BadRequestException.class)
  public void createSelectCompositionQueryBadRequestException() {
    when(templateService.createSelectCompositionQuery("1"))
            .thenThrow(new BadRequestException(TemplateService.class, CANNOT_FIND_TEMPLATE, String.format(CANNOT_FIND_TEMPLATE, 1)));
    templateService.createSelectCompositionQuery("1");
  }

  @Test(expected = SystemException.class)
  public void createSelectCompositionQuerySystemException() {
    ContainmentDto containmentDto = new ContainmentDto();
    containmentDto.setArchetypeId("1");
    when(aqlEditorContainmentService.buildContainment("1")).thenReturn(containmentDto);
    when(templateService.createSelectCompositionQuery("1"))
            .thenThrow(new SystemException(TemplateService.class, CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID,
                    String.format(CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID, 1)));
    templateService.createSelectCompositionQuery("1");
  }
}
