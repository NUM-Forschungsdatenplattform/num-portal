package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_FIND_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import de.vitagroup.num.service.exception.SystemException;
import org.ehrbase.aqleditor.dto.containment.ContainmentDto;
import org.ehrbase.aqleditor.service.AqlEditorContainmentService;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.mapper.TemplateMapper;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.BadRequestException;

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
