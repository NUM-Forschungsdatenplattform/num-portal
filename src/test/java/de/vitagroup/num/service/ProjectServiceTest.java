package de.vitagroup.num.service;

import static de.vitagroup.num.domain.ProjectStatus.ARCHIVED;
import static de.vitagroup.num.domain.ProjectStatus.CLOSED;
import static de.vitagroup.num.domain.ProjectStatus.PUBLISHED;
import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_ARCHIVE_PROJECT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_DELETE_PROJECT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.domain.specification.ProjectSpecification;
import de.vitagroup.num.mapper.ProjectMapper;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.cohort.CohortService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.ehrbase.ResponseFilter;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.PrivacyException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.ProjectCloseNotification;
import de.vitagroup.num.service.notification.dto.ProjectStartNotification;
import de.vitagroup.num.service.notification.dto.ProjectStatusChangeRequestNotification;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorSymbol;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.parser.AqlParseException;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.ehrbase.client.aql.field.EhrFields;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class ProjectServiceTest {

  private static final String CORONA_TEMPLATE = "Corona_Anamnese";
  private static final String EHR_ID_1 = "f4da8646-8e36-4d9d-869c-af9dce5935c7";
  private static final String EHR_ID_2 = "61861e76-1606-48c9-adcf-49ebbb2c6bbd";
  private static final String EHR_ID_3 = "47dc21a2-7076-4a57-89dc-bd83729ed52f";
  private static final String QUERY_1 =
      "SELECT e/ehr_id/value, "
          + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0005]/value/value, "
          + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0004]/value/value "
          + "FROM EHR e "
          + "contains COMPOSITION c3[openEHR-EHR-COMPOSITION.report.v1] "
          + "contains SECTION s4[openEHR-EHR-SECTION.adhoc.v1] "
          + "contains OBSERVATION o[openEHR-EHR-OBSERVATION.symptom_sign_screening.v0]";
  private static final String QUERY_2 =
      "Select e/ehr_id/value as F1, "
          + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0005]/value/value as F2, "
          + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0004]/value/value as F3 from EHR e "
          + "contains SECTION s4[openEHR-EHR-SECTION.adhoc.v1] "
          + "contains OBSERVATION o[openEHR-EHR-OBSERVATION.symptom_sign_screening.v0]";
  private static final String QUERY_BASIC = "SELECT e FROM EHR e";
  private static final String QUERY_3 =
      "SELECT c0 as openEHR_EHR_COMPOSITION_self_monitoring_v0, "
          + "c1 as openEHR_EHR_COMPOSITION_report_v1 FROM  "
          + "EHR e "
          + "contains (COMPOSITION c0[openEHR-EHR-COMPOSITION.self_monitoring.v0] and COMPOSITION c1[openEHR-EHR-COMPOSITION.report.v1])";
  private static final String QUERY_4 =
      "SELECT c0 as openEHR_EHR_COMPOSITION_self_monitoring_v0, "
          + "c1 as openEHR_EHR_COMPOSITION_report_v1 FROM  "
          + "EHR e "
          + "contains COMPOSITION c0[openEHR-EHR-COMPOSITION.self_monitoring.v0] "
          + "contains (COMPOSITION c2[openEHR-EHR-COMPOSITION.self_monitoring.v0] and COMPOSITION c1[openEHR-EHR-COMPOSITION.report.v1])";
  private static final String QUERY_5 =
      "SELECT c0 as openEHR_EHR_COMPOSITION_self_monitoring_v0, c1 as openEHR_EHR_COMPOSITION_report_v1 FROM EHR e contains (COMPOSITION c0[openEHR-EHR-COMPOSITION.self_monitoring.v0] and COMPOSITION c2[openEHR-EHR-COMPOSITION.self_monitoring.v0] and COMPOSITION c1[openEHR-EHR-COMPOSITION.report.v1])";

  @Captor ArgumentCaptor<AqlDto> aqlDtoArgumentCaptor;

  @Mock private ProjectRepository projectRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock private CohortService cohortService;

  @Mock private EhrBaseService ehrBaseService;

  @Mock private AtnaService atnaService;

  @Mock private NotificationService notificationService;

  @Mock private PrivacyProperties privacyProperties;

  @Mock private UserService userService;

  @Mock
  private ProjectMapper projectMapper;

  @Mock
  private ProjectDocCreator projectDocCreator;

  @Mock
  private TemplateService templateService;

  @Spy private ResponseFilter responseFilter;

  @Spy private ProjectPolicyService projectPolicyService;

  @Spy private ObjectMapper mapper;

  @InjectMocks private ProjectService projectService;

  @Spy private AqlEditorAqlService aqlEditorAqlService;

  @Captor ArgumentCaptor<List<Notification>> notificationCaptor;

  @Ignore(
      value = "This should pass when https://github.com/ehrbase/openEHR_SDK/issues/217 is fixed")
  @Test(expected = AqlParseException.class)
  public void shouldCorrectlyValidateInvalidQuery() {
    String query =
        "Select e/ehr_id/value as F1, c0 as F1 from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";
    new AqlToDtoParser().parse(query);
  }

  @Test
  @Ignore(
      value = "This should pass when https://github.com/ehrbase/openEHR_SDK/issues/216 is fixed")
  public void shouldCorrectlyGenerateAliases() {
    String query =
        "Select c0 as F1 from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";

    AqlDto dto = new AqlToDtoParser().parse(query);

    SelectFieldDto selectFieldDto = new SelectFieldDto();
    selectFieldDto.setAqlPath(EhrFields.EHR_ID().getPath());
    selectFieldDto.setContainmentId(dto.getEhr().getContainmentId());
    dto.getSelect().getStatement().add(0, selectFieldDto);

    String editedQuery = new AqlBinder().bind(dto).getLeft().buildAql();

    Matcher matcher = Pattern.compile("F1").matcher(editedQuery);

    int count = 0;
    while (matcher.find()) {
      count++;
    }

    assertThat(count, is(1));
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteProjectResourceNotFound() {
    when(projectService.deleteProject(1000000L, "1", List.of()))
            .thenThrow(new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, "1000000")));
    projectService.deleteProject(1000000L, "1", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void deleteProjectForbiddenException() {
    when(projectService.deleteProject(1L, "1", List.of()))
            .thenThrow(new ForbiddenException(ProjectService.class, CANNOT_DELETE_PROJECT, String.format(CANNOT_DELETE_PROJECT, 1)));
    projectService.deleteProject(1L, "1", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void deleteProjectForbiddenExceptionInvalidStatus() {
    when(projectService.deleteProject(1L, "1", List.of(Roles.SUPER_ADMIN)))
            .thenThrow(new ForbiddenException(ProjectService.class, CANNOT_DELETE_PROJECT, String.format(CANNOT_DELETE_PROJECT, 1)));
    projectService.deleteProject(1L, "1", List.of(Roles.SUPER_ADMIN));
  }

  @Test(expected = ResourceNotFound.class)
  public void archiveProjectResourceNotFound() {
    when(projectService.archiveProject(1000000L, "1", List.of()))
            .thenThrow(new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, "1000000")));
    projectService.archiveProject(1000000L, "1", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void archiveProjectForbiddenException() {
    when(projectService.archiveProject(1L, "1", List.of()))
            .thenThrow(new ForbiddenException(ProjectService.class, CANNOT_ARCHIVE_PROJECT, String.format(CANNOT_ARCHIVE_PROJECT, 1)));
    projectService.archiveProject(1L, "1", List.of());
  }

  @Test(expected = BadRequestException.class)
  public void archiveProjectBadRequestExceptionInvalidStatus() {
    when(projectService.archiveProject(1L, "1", List.of(Roles.SUPER_ADMIN)))
            .thenThrow(new BadRequestException(ProjectService.class, PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED,
                    String.format(PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED, PUBLISHED ,ARCHIVED )));
    projectService.archiveProject(1L, "1", List.of(Roles.SUPER_ADMIN));
  }

/*  @Test(expected = ForbiddenException.class)
  public void archiveProjectBadRequestExceptionInvalidStatusArchived() {
    when(projectService.archiveProject(5L, "1", List.of()))
            .thenThrow(new ForbiddenException(ProjectService.class, PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED,
                    String.format(PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED, PUBLISHED, ARCHIVED)));
    projectService.archiveProject(5L, "1", List.of());
  }*/

  @Test(expected = ResourceNotFound.class)
  public void retrieveDataResourceNotFound() {
    projectService.retrieveData("query", 10000L, "1", Boolean.TRUE);
  }

  @Test(expected = ForbiddenException.class)
  public void retrieveDataForbiddenException() {
    projectService.retrieveData("query", 1L, "1", Boolean.TRUE);
  }

  @Test(expected = ForbiddenException.class)
  public void retrieveDataForbiddenExceptionWrongProjectStatus() {
    projectService.retrieveData("query", 5L, "1", Boolean.TRUE);
  }

  @Test(expected = BadRequestException.class)
  public void retrieveDataBadRequestExceptionWrongCohort() {
    projectService.retrieveData("query", 6L, "ownerCoordinatorId", Boolean.TRUE);
  }

  @Test(expected = BadRequestException.class)
  public void retrieveDataBadRequestExceptionWrongTemplates() {
    projectService.retrieveData("query", 7L, "ownerCoordinatorId", Boolean.TRUE);
  }

  @Test(expected = PrivacyException.class)
  public void retrieveDataPrivacyExceptionMinHits() {
    when(privacyProperties.getMinHits()).thenReturn(10);
    projectService.retrieveData("query", 8L, "ownerCoordinatorId", Boolean.TRUE);
  }

  @Test(expected = SystemException.class)
  public void executeManagerProjectSystemException() throws JsonProcessingException {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();
    when(mapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("Error"){});
    projectService.executeManagerProject(cohortDto, Arrays.asList("1", "2"), "ownerCoordinatorId");
  }

  @Test(expected = BadRequestException.class)
  public void getResearchersBadRequestException() {
    ProjectDto project =
            ProjectDto.builder().name("Project")
                    .status(ProjectStatus.DRAFT)
                    .researchers(Arrays.asList(new UserDetailsDto()))
                    .build();

    projectService.createProject(project, "ownerCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test(expected = BadRequestException.class)
  public void getNotApprovedResearchersBadRequestException() {
    UserDetailsDto userDetailsDto = UserDetailsDto.builder()
            .userId("1")
            .approved(Boolean.FALSE).build();
    ProjectDto project =
            ProjectDto.builder().name("Project")
                    .status(ProjectStatus.DRAFT)
                    .researchers(Arrays.asList(userDetailsDto))
                    .build();

    when(userDetailsService.getUserDetailsById("1")).thenReturn(Optional.of(UserDetails.builder().userId("1").approved(Boolean.FALSE).build()));

    projectService.createProject(project, "ownerCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test(expected = BadRequestException.class)
  public void getInfoDocBytes() {
    projectService.getInfoDocBytes(1000000L, "ownerCoordinatorId", Locale.ENGLISH);
  }

  @Test//(expected = SystemException.class)
  public void getInfoDocBytesSystemException() throws IOException {
//    when(projectDocCreator.getDocBytesOfProject(new ProjectDto(), null)).thenThrow(new SystemException(ProjectService.class, ""){});
    projectService.getInfoDocBytes(1L, "ownerCoordinatorId", null);
  }

  @Test
  public void shouldBeConsistentInParsingAql() {
    String initialQuery =
        "SELECT c0 as openEHR_EHR_COMPOSITION_self_monitoring_v0, c1 as openEHR_EHR_COMPOSITION_report_v1 "
            + "FROM EHR e contains (COMPOSITION c0[openEHR-EHR-COMPOSITION.self_monitoring.v0] "
            + "and COMPOSITION c1[openEHR-EHR-COMPOSITION.report.v1]) "
            + "WHERE (e/ehr_id/value matches {'b3a40b41-36e1-4802-8748-062d4000aaae'} "
            + "and c0/archetype_details/template_id/value matches {'Corona_Anamnese'} "
            + "and c1/archetype_details/template_id/value matches {'Corona_Anamnese'})";

    AqlDto initialDto = new AqlToDtoParser().parse(initialQuery);
    assertThat(initialDto.getWhere(), notNullValue());
    assertThat(((ConditionLogicalOperatorDto) initialDto.getWhere()).getValues(), notNullValue());
    assertThat(((ConditionLogicalOperatorDto) initialDto.getWhere()).getValues().size(), is(3));

    String initialDtoToString = new AqlBinder().bind(initialDto).getLeft().buildAql();
    AqlDto parsedQuery = new AqlToDtoParser().parse(initialDtoToString);

    assertThat(parsedQuery.getWhere(), notNullValue());
    assertThat(((ConditionLogicalOperatorDto) parsedQuery.getWhere()).getValues(), notNullValue());
    assertThat(((ConditionLogicalOperatorDto) parsedQuery.getWhere()).getValues().size(), is(3));
  }

  @Test
  public void shouldHandleQuery5() {

    AqlDto initialQueryDto = new AqlToDtoParser().parse(QUERY_5);
    assertThat(initialQueryDto, notNullValue());
    assertThat(initialQueryDto.getWhere(), nullValue());

    projectService.executeAql(QUERY_5, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    assertThat(restrictedQuery.getWhere(), notNullValue());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldNotExecuteIfProjectNotPublished() {
    when(projectRepository.findById(7L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(7L)
                    .status(ProjectStatus.DENIED)
                    .cohort(Cohort.builder().id(4L).build())
                    .build()));
    projectService.executeAql(QUERY_5, 7L, "approvedCoordinatorId");
  }

  @Test
  public void shouldHandleQuery3() {
    projectService.executeAql(QUERY_3, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    ConditionLogicalOperatorDto conditionDto0 =
        (ConditionLogicalOperatorDto) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(3));
    ConditionLogicalOperatorDto conditionDto =
        (ConditionLogicalOperatorDto) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesOperatorDto));

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((SimpleValue) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test
  public void shouldHandleQuery4() {
    projectService.executeAql(QUERY_4, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    ConditionLogicalOperatorDto conditionDto0 =
        (ConditionLogicalOperatorDto) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(4));
    ConditionLogicalOperatorDto conditionDto =
        (ConditionLogicalOperatorDto) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesOperatorDto));

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((SimpleValue) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleStudyWithoutTemplates() {
    projectService.executeAql(QUERY_1, 1L, "approvedCoordinatorId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleStudyWithoutCohort() {
    projectService.executeAql(QUERY_1, 3L, "approvedCoordinatorId");
  }

  @Test
  public void shouldCorrectlyRestrictQueryWithContainsAndNoComposition() {
    projectService.executeAql(QUERY_2, 4L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQueryDto = aqlDtoArgumentCaptor.getValue();
    String restrictedQuery = new AqlBinder().bind(restrictedQueryDto).getLeft().buildAql();

    new AqlToDtoParser().parse(restrictedQuery);

    String expectedQuery =
        "Select e/ehr_id/value as F1, "
            + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0005]/value/value as F2, "
            + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0004]/value/value as F3 "
            + "from EHR e "
            + "contains (COMPOSITION c0 and SECTION s4[openEHR-EHR-SECTION.adhoc.v1] "
            + "contains OBSERVATION o[openEHR-EHR-OBSERVATION.symptom_sign_screening.v0]) "
            + "where (e/ehr_id/value matches {'47dc21a2-7076-4a57-89dc-bd83729ed52f'} and c0/archetype_details/template_id/value matches {'Corona_Anamnese'})";

    assertEquals(restrictedQuery, expectedQuery);
  }

  @Test
  public void shouldCorrectlyRestrictBasicQuery() {
    projectService.executeAql(QUERY_BASIC, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQueryDto = aqlDtoArgumentCaptor.getValue();
    String restrictedQuery = new AqlBinder().bind(restrictedQueryDto).getLeft().buildAql();

    new AqlToDtoParser().parse(restrictedQuery);
  }

  @Test
  public void shouldCorrectlyRestrictQuery() {
    projectService.executeAql(QUERY_1, 2L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlDto restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery.getWhere(), notNullValue());
    ConditionLogicalOperatorDto conditionDto0 =
        (ConditionLogicalOperatorDto) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(2));
    ConditionLogicalOperatorDto conditionDto =
        (ConditionLogicalOperatorDto) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesOperatorDto));

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertEquals(2, ehrMatches.getValues().size());

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((SimpleValue) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test
  public void shouldFailExecutingAndLogWithUnapproved() {
    String query =
        "Select o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Systolic__magnitude, e/ehr_id/value as ehr_id from EHR e contains OBSERVATION o0[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1] where (o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude >= $magnitude and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude < 1.1)";
    Exception exception = null;
    try {
      projectService.executeAql(query, 1L, "notApprovedCoordinatorId");
    } catch (Exception e) {
      exception = e;
    }

    Mockito.verify(atnaService, times(1))
        .logDataExport(eq("notApprovedCoordinatorId"), eq(1L), eq(null), eq(false));

    assertTrue(exception instanceof ForbiddenException);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingCoordinator() {
    ProjectDto project =
        ProjectDto.builder().name("Project").status(ProjectStatus.APPROVED).build();

    projectService.createProject(project, "nonExistingCoordinatorId", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedCoordinator() {
    ProjectDto projectDto = ProjectDto.builder().name("Project").build();

    projectService.createProject(projectDto, "notApprovedCoordinatorId", List.of());
  }

  @Test
  public void shouldFilterWhenSearchingStudiesWithCoordinator() {
    List<String> roles = new ArrayList<>();
    roles.add(Roles.STUDY_COORDINATOR);
    projectService.getProjects("coordinatorId", roles);

    verify(projectRepository, times(1))
        .findByCoordinatorUserIdOrStatusIn(
            "coordinatorId",
            ProjectStatus.getAllProjectStatusToViewAsCoordinator());
    verify(projectRepository, times(0)).findAll();
  }

  @Test
  public void shouldFilterWhenSearchingProjectsWithResearcher() {
    List<String> roles = new ArrayList<>();
    roles.add(RESEARCHER);
    projectService.getProjects("researcherId", roles);

    verify(projectRepository, times(1))
            .findByResearchers_UserIdAndStatusIn(
                    "researcherId",
                    ProjectStatus.getAllProjectStatusToViewAsResearcher());
  }

  @Test
  public void shouldFilterWhenSearchingProjectsWithApprover() {
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_APPROVER);
    projectService.getProjects("approverId", roles);

    verify(projectRepository, times(1))
            .findByStatusIn(ProjectStatus.getAllProjectStatusToViewAsApprover());
  }

  @Test
  public void getAllProjectsWithPagination() {
    setupDataForProjectsWithPagination();
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_COORDINATOR);
    Pageable pageable = PageRequest.of(0,100).withSort(Sort.by(Sort.Direction.DESC, "name"));
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    List<Project> projects = projectService.getProjectsWithPagination("approvedCoordinatorId", roles,
                    SearchCriteria.builder()
                                  .sort("DESC")
                                  .sortBy("name")
                                  .build(), pageable).getContent();
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    Assert.assertEquals(Long.valueOf(1L), projects.get(0).getId());
    Assert.assertEquals("approvedCoordinatorId", specificationArgumentCaptor.getValue().getLoggedInUserId());
    Assert.assertEquals(roles, specificationArgumentCaptor.getValue().getRoles());
    Assert.assertNull(specificationArgumentCaptor.getValue().getFilter());
  }

  @Test
  public void getAllProjectsWithPaginationAndSortByOrganization() {
    setupDataForProjectsWithPagination();
    Pageable pageable = PageRequest.of(0,100);
    Page<Project> filteredProjects = projectService.getProjectsWithPagination("approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR),
            SearchCriteria.builder()
                    .sort("ASC")
                    .sortBy("organization")
                    .build(), pageable);
    Mockito.verify(projectRepository, times(1)).findProjects(Mockito.any(ProjectSpecification.class), Mockito.eq(pageable));
    List<Project> projects = filteredProjects.getContent();
    Assert.assertEquals(Long.valueOf(2L), projects.get(0).getId());
  }

  @Test
  public void getAllProjectsWithPaginationAndFilter() {
    setupDataForProjectsWithPagination();
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_COORDINATOR);
    roles.add(RESEARCHER);
    Pageable pageable = PageRequest.of(0,100).withSort(Sort.by(Sort.Direction.ASC, "name"));
    Map<String, String> filter = new HashMap<>();
    filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "OnE");
    filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.OWNED.name());
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    Set<String> owners = new HashSet<>();
    owners.add("approvedCoordinator");
    Mockito.when(userService.findUsersUUID(Mockito.eq("OnE"), Mockito.anyInt(), Mockito.eq(100))).thenReturn(owners);
    Page<Project> filteredProjects = projectService.getProjectsWithPagination("approvedCoordinatorId", roles,
            SearchCriteria.builder()
                    .sort("ASC")
                    .sortBy("name")
                    .filter(filter)
                    .build(), pageable);
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    List<Project> projects = filteredProjects.getContent();
    Assert.assertEquals(Long.valueOf(1L), projects.get(0).getId());
    ProjectSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(filter, capturedInput.getFilter());
    Assert.assertEquals("approvedCoordinatorId", capturedInput.getLoggedInUserId());
    Assert.assertEquals(roles, capturedInput.getRoles());
    Assert.assertEquals(owners, capturedInput.getOwnersUUID());
  }

  @Test
  public void getAllProjectsWithPaginationAndSortByAuthor() {
    setupDataForProjectsWithPagination();
    when(userService.getOwner("approvedCoordinatorId")).thenReturn(User.builder().id("approvedCoordinatorId").firstName("AA Coordinator first name").build());
    Pageable pageable = PageRequest.of(0,100);
    Page<Project> filteredProjects = projectService.getProjectsWithPagination("approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR),
            SearchCriteria.builder()
                    .sort("DESC")
                    .sortBy("author")
                    .build(), pageable);
    Mockito.verify(projectRepository, times(1)).findProjects(Mockito.any(ProjectSpecification.class), Mockito.eq(pageable));
    List<Project> projects = filteredProjects.getContent();
    Assert.assertEquals(Long.valueOf(2L), projects.get(0).getId());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleInvalidSortWhenGetProjectsWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sort("dummyName")
            .sortBy("ASC")
            .build();
    when(userDetailsService.getUserDetailsById("approvedCoordinatorId"))
            .thenReturn(Optional.of(UserDetails.builder().build()));
    projectService.getProjectsWithPagination("approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR), searchCriteria, pageable);
    verify(projectRepository, never());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingSortFieldWhenGetProjectsWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sortBy("ASC")
            .build();
    when(userDetailsService.getUserDetailsById("approvedCoordinatorId"))
            .thenReturn(Optional.of(UserDetails.builder().build()));
    projectService.getProjectsWithPagination("approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR), searchCriteria, pageable);
    verify(projectRepository, never());
  }

  @Test
  public void getAllProjectsWithStudyApprover() {
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_APPROVER);
    setupDataForProjectsWithPagination();
    when(userDetailsService.getUserDetailsById("approverId"))
            .thenReturn(Optional.of(UserDetails.builder()
                    .userId("approverId")
                    .approved(true)
                    .organization(Organization.builder().id(1L).build())
                    .build()));
    Pageable pageable = PageRequest.of(0,50).withSort(Sort.by(Sort.Direction.DESC, "status"));
    Map<String, String> filter = new HashMap<>();
    filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ORGANIZATION.name());
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    projectService.getProjectsWithPagination("approverId", roles,
            SearchCriteria.builder()
                    .sort("DESC")
                    .sortBy("status")
                    .filter(filter)
                    .build(), pageable);
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    ProjectSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(filter, capturedInput.getFilter());
    Assert.assertEquals("approverId", capturedInput.getLoggedInUserId());
    Assert.assertEquals(roles, capturedInput.getRoles());
    assertThat(1L, is(capturedInput.getLoggedInUserOrganizationId()));
  }

  private void setupDataForProjectsWithPagination() {
    Organization orgOne = Organization.builder()
            .id(1L)
            .name("aa some organization name")
            .build();
    Organization orgTwo = Organization.builder()
            .id(2L)
            .name("bb some organization name")
            .build();
    UserDetails coordinator = UserDetails.builder()
            .userId("approvedCoordinatorId")
            .approved(true)
            .organization(orgTwo)
            .build();
    UserDetails anotherCoordinator = UserDetails.builder()
            .userId("anotherCoordinatorId")
            .approved(true)
            .organization(orgOne)
            .build();
    when(userDetailsService.getUserDetailsById("approvedCoordinatorId"))
            .thenReturn(Optional.of(coordinator));
    Project pr1 = Project.builder().id(1L)
            .name("project name one")
            .status(ProjectStatus.APPROVED)
            .coordinator(coordinator)
            .build();
    Project pr2 = Project.builder().id(2L)
            .name("project name two")
            .status(ProjectStatus.PUBLISHED)
            .coordinator(anotherCoordinator)
            .build();
    Project pr3 = Project.builder().id(3L)
            .name("project name blaaa")
            .status(ProjectStatus.PUBLISHED)
            .coordinator(anotherCoordinator)
            .build();
    when(userService.getOwner("anotherCoordinatorId")).thenReturn(User.builder().id("anotherCoordinatorId").firstName("Coordinator first name").build());
    Mockito.when(projectRepository.findProjects(Mockito.any(ProjectSpecification.class), Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(pr1, pr2, pr3)));
  }

  @Test
  public void shouldHandleMissingProject() {
    Optional<Project> project = projectService.getProjectById(19L);

    assertThat(project, notNullValue());
    assertThat(project.isEmpty(), is(true));
  }

  @Test
  public void shouldRejectStudyDraftToApprovedTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.APPROVED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                projectService.updateProject(
                    projectDto,
                    1L,
                    "approvedCoordinatorId",
                    List.of(STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER)));

    String expectedMessage = "Project status transition from DRAFT to APPROVED is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectStudyDraftToPublishedTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(PUBLISHED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from DRAFT to PUBLISHED is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectStudyDraftToClosedTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(CLOSED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from DRAFT to CLOSED is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectStudyClosedToApprovedTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(CLOSED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.APPROVED).build();

    projectService.updateProject(projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectStudyClosedToDraftTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(CLOSED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.DRAFT).build();

    projectService.updateProject(
        projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR, STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyDraftToPendingTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.PENDING).build();

    projectService.updateProject(
        projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldAllowStudyPendingToDraftTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.DRAFT).build();

    projectService.updateProject(
        projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldAllowStudyPendingToReviewingTransitionToApprover() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.REVIEWING).build();

    projectService.updateProject(projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldDenyStudyPendingToReviewingTransitionToCoordinator() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.REVIEWING).build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from PENDING to REVIEWING is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowStudyReviewingToApprovedTransitionToApprover() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .status(ProjectStatus.APPROVED)
            .financed(false)
            .build();

    projectService.updateProject(projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyReviewingToApprovedTransitionToCoordinator() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .financed(false)
            .status(ProjectStatus.APPROVED)
            .build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from REVIEWING to APPROVED is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowStudyReviewingToChangeRequestTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .financed(false)
            .status(ProjectStatus.CHANGE_REQUEST)
            .build();

    projectService.updateProject(projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyReviewingToDeniedTransitionToApprover() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .financed(false)
            .status(ProjectStatus.DENIED)
            .build();

    projectService.updateProject(projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldDenyStudyReviewingToDeniedTransitionToCoordinatorAndResearcher() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .financed(false)
            .status(ProjectStatus.DENIED)
            .build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                projectService.updateProject(
                    projectDto,
                    1L,
                    "approvedCoordinatorId",
                    List.of(STUDY_COORDINATOR, RESEARCHER)));

    String expectedMessage = "Project status transition from REVIEWING to DENIED is not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialClosedStudyStatus() {
    ProjectDto newStudy =
        ProjectDto.builder()
            .name("new project")
            .financed(false)
            .status(CLOSED)
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> projectService.createProject(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid project status: CLOSED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialApprovedStudyStatus() {
    ProjectDto newStudy =
        ProjectDto.builder()
            .name("new project")
            .financed(false)
            .status(ProjectStatus.APPROVED)
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> projectService.createProject(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid project status: APPROVED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowInitialDraftStudyStatus() {
    ProjectDto newStudy =
        ProjectDto.builder()
            .name("new project")
            .financed(false)
            .status(ProjectStatus.DRAFT)
            .build();
    projectService.createProject(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).save(any());
  }

  @Test
  public void shouldAllowInitialPendingStudyStatus() {
    ProjectDto newStudy =
        ProjectDto.builder()
            .name("new Project")
            .financed(false)
            .status(ProjectStatus.PENDING)
            .build();
    projectService.createProject(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).save(any());
  }

  @Test
  public void shouldAllowEditingOwnedStudy() {
    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    ProjectDto existingStudy =
        ProjectDto.builder()
            .id(1L)
            .name("existing Project")
            .financed(false)
            .status(ProjectStatus.PENDING)
            .build();
    projectService.updateProject(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectEditingNotOwnedStudy() {
    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .coordinator(new UserDetails("ownerCoordinatorId", null, true))
                    .build()));

    ProjectDto existingStudy =
        ProjectDto.builder()
            .id(1L)
            .name("existing Project")
            .financed(false)
            .status(ProjectStatus.PENDING)
            .build();
    projectService.updateProject(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldOnlyAllowEditingOfResearchersAfterApprovedState() {
    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .name("oldName")
                    .status(ProjectStatus.APPROVED)
                    .researchers(
                        Collections.singletonList(
                            UserDetails.builder().userId("2").approved(true).build()))
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    when(userDetailsService.getUserDetailsById("1"))
        .thenReturn(Optional.of(UserDetails.builder().userId("1").approved(true).build()));

    ProjectDto existingStudy =
        ProjectDto.builder()
            .id(1L)
            .name("existing Project")
            .status(ProjectStatus.APPROVED)
            .researchers(
                Collections.singletonList(
                    UserDetailsDto.builder().userId("1").approved(true).build()))
            .financed(false)
            .build();
    Project returnedProject =
        projectService.updateProject(
            existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));

    assertThat(returnedProject.getName(), is("oldName"));
    assertThat(returnedProject.getResearchers().get(0).getUserId(), is("1"));

    verify(projectRepository, times(1)).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectEditingClosedStudies() {
    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .status(CLOSED)
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    ProjectDto existingStudy =
        ProjectDto.builder()
            .id(1L)
            .name("existing Project")
            .financed(false)
            .status(CLOSED)
            .build();
    projectService.updateProject(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).save(any());
  }

  @Test
  public void shouldSuccessfullyExecuteManagerProject() {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();

    UserDetails userDetails =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    String result =
        projectService.executeManagerProject(
            cohortDto, List.of(CORONA_TEMPLATE), userDetails.getUserId());

    assertThat(result, is("[]"));
  }

  @Test
  public void shouldSendNotificationWhenProjectStarts() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .id(66L)
            .status(ProjectStatus.APPROVED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .researchers(
                List.of(
                    UserDetails.builder().userId("researcher1").build(),
                    UserDetails.builder().userId("researcher2").build()))
            .build();

    when(projectRepository.findById(66L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .id(66L)
            .status(PUBLISHED)
            .coordinator(User.builder().id("approvedCoordinatorId").build())
            .researchers(
                List.of(
                    UserDetailsDto.builder().userId("researcher1").build(),
                    UserDetailsDto.builder().userId("researcher1").build()))
            .build();

    projectService.updateProject(
        projectDto,
        66L,
        "approvedCoordinatorId",
        List.of(STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER));

    verify(projectRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(2));
    assertThat(notificationSent.get(0).getClass(), is(ProjectStartNotification.class));
  }

  @Test
  public void shouldSendNotificationWhenRemovingResearchers() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .id(66L)
            .status(PUBLISHED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .researchers(
                List.of(
                    UserDetails.builder().userId("researcher1").build(),
                    UserDetails.builder().userId("researcher2").build()))
            .build();

    when(projectRepository.findById(66L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .id(66L)
            .status(PUBLISHED)
            .coordinator(User.builder().id("approvedCoordinatorId").build())
            .researchers(List.of(UserDetailsDto.builder().userId("researcher1").build()))
            .build();

    projectService.updateProject(
        projectDto,
        66L,
        "approvedCoordinatorId",
        List.of(STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER));

    verify(projectRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(ProjectCloseNotification.class));
  }

  @Test
  public void shouldSendNotificationWhenClosingProject() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .id(77L)
            .status(PUBLISHED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .researchers(
                List.of(
                    UserDetails.builder().userId("researcher1").build(),
                    UserDetails.builder().userId("researcher2").build()))
            .build();

    when(projectRepository.findById(77L)).thenReturn(Optional.of(projectToEdit));

    ProjectDto projectDto =
        ProjectDto.builder()
            .name("Project is edited")
            .id(77L)
            .status(CLOSED)
            .coordinator(User.builder().id("approvedCoordinatorId").build())
            .researchers(List.of(UserDetailsDto.builder().userId("researcher1").build()))
            .build();

    projectService.updateProject(
        projectDto,
        77L,
        "approvedCoordinatorId",
        List.of(STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER));

    verify(projectRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(2));
    assertThat(notificationSent.get(0).getClass(), is(ProjectCloseNotification.class));
  }

  @Test
  public void shouldSendNotificationWhenChangeRequestProject() {
      Project projectEntity = Project.builder().id(99L)
              .name("Project T001")
              .status(ProjectStatus.REVIEWING)
              .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
              .researchers(List.of(UserDetails.builder().userId("researcher1").build()))
              .build();
      when(projectRepository.findById(99L)).thenReturn(Optional.of(projectEntity));

    ProjectDto projectDto =
            ProjectDto.builder()
                    .name("Project T001")
                    .id(99L)
                    .status(ProjectStatus.CHANGE_REQUEST)
                    .coordinator(User.builder().id("approvedCoordinatorId").build())
                    .researchers(List.of(UserDetailsDto.builder().userId("researcher1").build()))
                    .build();
    projectService.updateProject(
            projectDto,
            99L,
            "approvedCoordinatorId",
            List.of(STUDY_APPROVER));

    verify(projectRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(ProjectStatusChangeRequestNotification.class));
  }

  @Test
  public void shouldGetLatestProjectsInfoTest() {
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_COORDINATOR);

    Project pr1 = Project.builder().id(1L)
            .name("project one")
            .status(ProjectStatus.APPROVED)
            .coordinator(UserDetails.builder()
                    .userId("approvedCoordinatorId")
                    .approved(true)
                    .organization(Organization.builder()
                            .name("some organization")
                            .id(3L).build())
                    .build())
            .build();
    Project pr2 = Project.builder().id(2l)
            .name("project two")
            .status(ProjectStatus.PUBLISHED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").approved(true).build())
            .build();
    Mockito.when(projectRepository.findLatestProjects(10, ProjectStatus.APPROVED.name(),
                 ProjectStatus.PUBLISHED.name(), ProjectStatus.CLOSED.name()))
                .thenReturn(Arrays.asList(pr1,pr2));
    projectService.getLatestProjectsInfo(10, roles);
    verify(projectRepository, times(1)).findLatestProjects(10, ProjectStatus.APPROVED.name(),
            ProjectStatus.PUBLISHED.name(), ProjectStatus.CLOSED.name());
  }

  @Test
  public void shouldReturnNoProjectsWhenCounterLessOneTest() {
    List<ProjectInfoDto> projects = projectService.getLatestProjectsInfo(0, Arrays.asList(STUDY_COORDINATOR));
    Mockito.verifyNoInteractions(projectRepository);
    Assert.assertTrue(projects.isEmpty());
  }

  @Test
  public void deleteProjectTest() {
    String userId = "approvedCoordinatorId";
    UserDetails userDetails = UserDetails.builder()
            .userId(userId)
            .approved(true)
            .build();
    when(projectRepository.findById(9L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(3L)
                                    .status(ProjectStatus.CHANGE_REQUEST)
                                    .coordinator(userDetails)
                                    .build()));
    projectService.deleteProject(9L, userId, Arrays.asList(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).deleteById(9L);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectDeleteNotOwnedProject() {
    when(projectRepository.findById(1L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(1L)
                                    .coordinator(UserDetails.builder().userId("some-user-id").approved(true).build())
                                    .build()));

    projectService.deleteProject(1L, "approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR));
    verify(projectRepository, times(0)).deleteById(1L);
  }

  @Test
  public void archiveProjectTest() {
    when(projectRepository.findById(5L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(5L)
                                    .status(ProjectStatus.CLOSED)
                                    .coordinator(UserDetails.builder().userId("approvedCoordinatorId").approved(true).build())
                                    .build()));
    projectService.archiveProject(5L, "approvedCoordinatorId", Arrays.asList(STUDY_COORDINATOR));
    verify(projectRepository, times(1)).save(any());
  }

  @Test
  public void countProjectsTest() {
    projectService.countProjects();
    verify(projectRepository, times(1)).count();
  }

  @Test
  public void retrieveDataTest() {
    projectService.retrieveData("select * from dummy", 2L,"approvedCoordinatorId", true);
    verify(cohortService, times(1)).executeCohort(Mockito.any(Cohort.class), Mockito.eq(false));
  }

  @Test
  public void getExportFilenameBodyTest() {
    String currentDate = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String expected = "Project_3_" + currentDate.replace("-","_");
    String projectFilename = projectService.getExportFilenameBody(3L);
    Assert.assertEquals(expected, projectFilename);
  }

  @Test
  public void getInfoDocBytesTest() throws IOException {
    when(projectMapper.convertToDto(Mockito.any(Project.class))).thenReturn(ProjectDto.builder().id(3L).build());
    projectService.getInfoDocBytes(3L, "approvedCoordinator", Locale.GERMAN);
    verify(projectDocCreator, times(1)).getDocBytesOfProject(Mockito.any(ProjectDto.class), Mockito.eq(Locale.GERMAN));
  }

  @Test
  public void getExportHeadersAsJsonTest() {
    MultiValueMap<String, String> headers = projectService.getExportHeaders(ExportType.json, 3L);
    Assert.assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
  }

  @Test
  public void getExportHeadersAsCSVTest() {
    MultiValueMap<String, String> headers = projectService.getExportHeaders(ExportType.csv, 3L);
    Assert.assertEquals("application/zip", headers.getFirst(HttpHeaders.CONTENT_TYPE));
  }

  @Test
  public void getExportResponseBodyAsJsonTest() {
    AqlDto aqlDto = new AqlToDtoParser().parse(QUERY_5);
    when(templateService.createSelectCompositionQuery(Mockito.any())).thenReturn(aqlDto);
    projectService.getExportResponseBody("select * from dummy", 2L, "approvedCoordinatorId", ExportType.json, true);
    Mockito.verify(cohortService, times(1)).executeCohort(Mockito.any(Cohort.class), Mockito.eq(false));
  }

  @Test
  public void streamResponseBody() throws IOException {
    QueryResponseData response = new QueryResponseData();
    response.setName("response-one");
    response.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
    response.setRows(  List.of(
            new ArrayList<>(List.of("ehr-id-1", Map.of("_type", "OBSERVATION", "uuid", "12345"))),
            new ArrayList<>(List.of("ehr-id-2", Map.of("_type", "SECTION", "uuid", "bla")))));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    projectService.streamResponseAsZip(Arrays.asList(response), "testFile", out);

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry expectedFile = zipInputStream.getNextEntry();
    Assert.assertEquals("testFile_response-one.csv", expectedFile.getName());
  }

  @Before
  public void setup() {
    when(userDetailsService.getUserDetailsById("researcher1"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("researcher1").approved(true).build()));

    when(userService.getUserById("researcher2", false))
        .thenReturn(
            User.builder()
                .id("researcher2")
                .firstName("f2")
                .lastName("l2")
                .email("em2@vitagroup.ag")
                .build());

    when(userService.getUserById("researcher1", false))
        .thenReturn(
            User.builder()
                .id("researcher1")
                .firstName("f1")
                .lastName("l1")
                .email("em1@vitagroup.ag")
                .build());

    UserDetails approvedCoordinator =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    User approvedUser = User.builder().id("approvedCoordinatorId").approved(true).build();

    when(userService.getUserById("approvedCoordinatorId", false)).thenReturn(approvedUser);

    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
        .thenReturn(approvedCoordinator);

    when(userDetailsService.checkIsUserApproved("notApprovedCoordinatorId"))
        .thenThrow(new ForbiddenException(ProjectServiceTest.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED));

    when(userDetailsService.checkIsUserApproved("nonExistingCoordinatorId"))
        .thenThrow(new SystemException(ProjectServiceTest.class, USER_NOT_FOUND));

    when(projectRepository.findById(3L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(3L)
                    .status(PUBLISHED)
                    .researchers(List.of(approvedCoordinator))
                    .build()));

    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .status(PUBLISHED)
                    .cohort(Cohort.builder().id(2L).build())
                    .researchers(List.of(approvedCoordinator))
                    .build()));

    when(projectRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(2L)
                    .status(PUBLISHED)
                    .cohort(Cohort.builder().id(2L).build())
                    .researchers(List.of(approvedCoordinator))
                    .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
                    .build()));

    when(projectRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Project project = invocation.getArgument(0, Project.class);
              project.setId(1L);
              return project;
            });

    when(projectRepository.findById(4L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(4L)
                    .status(PUBLISHED)
                    .cohort(Cohort.builder().id(4L).build())
                    .researchers(List.of(approvedCoordinator))
                    .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
                    .build()));

    when(projectRepository.findById(5L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(5L)
                                    .cohort(Cohort.builder().id(5L).build())
                                    .build()));

    when(projectRepository.findById(6L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(6L)
                                    .status(PUBLISHED)
                                    .coordinator(new UserDetails("ownerCoordinatorId", null, true))
                                    .build()));

    when(projectRepository.findById(7L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(7L)
                                    .cohort(Cohort.builder().id(5L).build())
                                    .status(PUBLISHED)
                                    .coordinator(new UserDetails("ownerCoordinatorId", null, true))
                                    .build()));

    Map<String, String> map = new HashMap<>();
    map.put("1", "1");
    when(projectRepository.findById(8L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(8L)
                                    .cohort(Cohort.builder().id(8L).build())
                                    .status(PUBLISHED)
                                    .templates(map)
                                    .coordinator(new UserDetails("ownerCoordinatorId", null, true))
                                    .build()));

    when(cohortService.executeCohort(2L, false)).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
    when(cohortService.executeCohort(4L, false)).thenReturn(Set.of(EHR_ID_3));
    when(cohortService.executeCohort(any(), any())).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
    when(privacyProperties.getMinHits()).thenReturn(0);
  }
}
