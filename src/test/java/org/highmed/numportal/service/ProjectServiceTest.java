package org.highmed.numportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.highmed.numportal.attachment.service.AttachmentService;
import org.highmed.numportal.domain.dto.*;
import org.highmed.numportal.domain.model.*;
import org.highmed.numportal.service.exception.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.ProjectRepository;
import org.highmed.numportal.domain.specification.ProjectSpecification;
import org.highmed.numportal.mapper.ProjectMapper;
import org.highmed.numportal.properties.ConsentProperties;
import org.highmed.numportal.properties.PrivacyProperties;
import org.highmed.numportal.service.atna.AtnaService;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.ehrbase.ResponseFilter;
import org.highmed.numportal.service.notification.NotificationService;
import org.highmed.numportal.service.notification.dto.Notification;
import org.highmed.numportal.service.notification.dto.ProjectCloseNotification;
import org.highmed.numportal.service.notification.dto.ProjectStartNotification;
import org.highmed.numportal.service.notification.dto.ProjectStatusChangeRequestNotification;
import org.highmed.numportal.service.policy.ProjectPolicyService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.highmed.numportal.domain.model.ProjectStatus.*;
import static org.highmed.numportal.domain.model.Roles.*;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

  @Captor ArgumentCaptor<AqlQuery> aqlDtoArgumentCaptor;

  @Mock private ProjectRepository projectRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock private CohortService cohortService;

  @Mock private EhrBaseService ehrBaseService;

  @Mock private AtnaService atnaService;

  @Mock private NotificationService notificationService;

  @Mock private PrivacyProperties privacyProperties;

  @Mock
  private ConsentProperties consentProperties;

  @Mock private UserService userService;

  @Mock
  private ProjectMapper projectMapper;

  @Mock
  private ProjectDocCreator projectDocCreator;

  @Mock
  private TemplateService templateService;

  @Mock
  private AttachmentService attachmentService;

  @Spy private ResponseFilter responseFilter;

  @Spy private ProjectPolicyService projectPolicyService;

  @Spy private ObjectMapper mapper;

  @InjectMocks private ProjectService projectService;

  @Spy private AqlEditorAqlService aqlEditorAqlService;

  @Captor ArgumentCaptor<List<Notification>> notificationCaptor;

  private Project projectOne;

  private ProjectDto projectDtoOne;

  @Ignore(
      value = "This should pass when https://github.com/ehrbase/openEHR_SDK/issues/217 is fixed")
  @Test(expected = AqlParseException.class)
  public void shouldCorrectlyValidateInvalidQuery() {
    String query =
        "Select e/ehr_id/value as F1, c0 as F1 from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";
    AqlQueryParser.parse(query);
  }

  @Test
  public void shouldCorrectlyGenerateAliases() {
    String query =
        "Select c0 as F1 from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";

    AqlQuery dto = AqlQueryParser.parse(query);

    String editedQuery = AqlRenderer.render(dto);

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

 @Test(expected = ForbiddenException.class)
  public void archiveProjectBadRequestExceptionNotAllowed() {
    projectService.archiveProject(5L, "1", List.of(STUDY_COORDINATOR));
  }

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
    projectService.retrieveData("query", 6L, "researcher2", Boolean.TRUE);
  }

  @Test(expected = BadRequestException.class)
  public void retrieveDataBadRequestExceptionWrongTemplates() {
    projectService.retrieveData("query", 7L, "researcher2", Boolean.TRUE);
  }

  @Test(expected = PrivacyException.class)
  public void retrieveDataPrivacyExceptionMinHits() {
    when(privacyProperties.getMinHits()).thenReturn(10);
    projectService.retrieveData("query", 8L, "researcher2", Boolean.TRUE);
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
                    .researchers(List.of(new UserDetailsDto()))
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
                    .researchers(List.of(userDetailsDto))
                    .build();

    when(userDetailsService.getUserDetailsById("1")).thenReturn(Optional.of(UserDetails.builder().userId("1").approved(Boolean.FALSE).build()));

    projectService.createProject(project, "ownerCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test(expected = BadRequestException.class)
  public void getInfoDocBytes() {
    projectService.getInfoDocBytes(1000000L, "ownerCoordinatorId", Locale.ENGLISH);
  }

    @Test
    public void getInfoDocBytesSystemException() throws IOException {
        when(projectDocCreator.getDocBytesOfProject(projectDtoOne, null))
                .thenThrow(new IOException());
        try {
            projectService.getInfoDocBytes(1L, "ownerCoordinatorId", null);
        } catch (SystemException se) {
            Assert.assertTrue(true);
            Assert.assertEquals(ERROR_CREATING_THE_PROJECT_PDF, se.getParamValue());
        }
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

    AqlQuery initialDto = AqlQueryParser.parse(initialQuery);
    assertThat(initialDto.getWhere(), notNullValue());
    assertThat(((LogicalOperatorCondition) initialDto.getWhere()).getValues(), notNullValue());
    assertThat(((LogicalOperatorCondition) initialDto.getWhere()).getValues().size(), is(3));

    String initialDtoToString = AqlRenderer.render(initialDto);
    AqlQuery parsedQuery = AqlQueryParser.parse(initialDtoToString);

    assertThat(parsedQuery.getWhere(), notNullValue());
    assertThat(((LogicalOperatorCondition) parsedQuery.getWhere()).getValues(), notNullValue());
    assertThat(((LogicalOperatorCondition) parsedQuery.getWhere()).getValues().size(), is(3));
  }

  @Test
  public void shouldHandleQuery5() {

    AqlQuery initialQueryDto = AqlQueryParser.parse(QUERY_5);
    assertThat(initialQueryDto, notNullValue());
    assertThat(initialQueryDto.getWhere(), nullValue());

    projectService.executeAql(QUERY_5, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlQuery restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    assertThat(restrictedQuery.getWhere(), notNullValue());
  }

    @Test
    public void shouldExecuteAqlForProjectOutsideEU() {
        projectService.executeAql(QUERY_5, 33L, "approvedCoordinatorId");
        Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
        AqlQuery restrictedQuery = aqlDtoArgumentCaptor.getValue();

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
    AqlQuery restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    LogicalOperatorCondition conditionDto0 =
        (LogicalOperatorCondition) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(3));
    LogicalOperatorCondition conditionDto =
        (LogicalOperatorCondition) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesCondition));

    MatchesCondition ehrMatches = (MatchesCondition) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_2)));

    MatchesCondition templatesMatches = (MatchesCondition) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((Primitive) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test
  public void shouldHandleQuery4() {
    projectService.executeAql(QUERY_4, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlQuery restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery, notNullValue());
    assertThat(restrictedQuery.getWhere(), notNullValue());

    LogicalOperatorCondition conditionDto0 =
        (LogicalOperatorCondition) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(4));
    LogicalOperatorCondition conditionDto =
        (LogicalOperatorCondition) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesCondition));

    MatchesCondition ehrMatches = (MatchesCondition) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_2)));

    MatchesCondition templatesMatches = (MatchesCondition) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((Primitive) t).getValue().equals(CORONA_TEMPLATE)));
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
    AqlQuery restrictedQueryDto = aqlDtoArgumentCaptor.getValue();
    String restrictedQuery = AqlRenderer.render(restrictedQueryDto);

    AqlQueryParser.parse(restrictedQuery);

    String expectedQuery =
        "SELECT e/ehr_id/value AS F1, "
            + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0005]/value/value AS F2, "
            + "o/data[at0001]/events[at0002]/data[at0003]/items[at0022]/items[at0004]/value/value AS F3 "
            + "FROM EHR e "
            + "CONTAINS (COMPOSITION c1 AND (SECTION s4[openEHR-EHR-SECTION.adhoc.v1] "
            + "CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.symptom_sign_screening.v0])) "
            + "WHERE ((e/ehr_id/value MATCHES {'47dc21a2-7076-4a57-89dc-bd83729ed52f'}) AND c1/archetype_details/template_id/value MATCHES {'Corona_Anamnese'})";

    assertEquals(restrictedQuery, expectedQuery);
  }

  @Test
  public void shouldCorrectlyRestrictBasicQuery() {
    projectService.executeAql(QUERY_BASIC, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlQuery restrictedQueryDto = aqlDtoArgumentCaptor.getValue();
    String restrictedQuery = AqlRenderer.render(restrictedQueryDto);

    AqlQueryParser.parse(restrictedQuery);
  }

  @Test
  public void shouldCorrectlyRestrictQuery() {
    projectService.executeAql(QUERY_1, 2L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(aqlDtoArgumentCaptor.capture(), any());
    AqlQuery restrictedQuery = aqlDtoArgumentCaptor.getValue();

    assertThat(restrictedQuery.getWhere(), notNullValue());
    LogicalOperatorCondition conditionDto0 =
        (LogicalOperatorCondition) restrictedQuery.getWhere();
    assertThat(conditionDto0.getSymbol(), is(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto0.getValues().size(), is(2));
    LogicalOperatorCondition conditionDto =
        (LogicalOperatorCondition) conditionDto0.getValues().get(0);
    assertThat(conditionDto.getValues().size(), is(1));

    assertTrue(
        conditionDto.getValues().stream()
            .anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesCondition));

    MatchesCondition ehrMatches = (MatchesCondition) conditionDto.getValues().get(0);
    assertEquals(2, ehrMatches.getValues().size());

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((Primitive) e).getValue().equals(EHR_ID_2)));

    MatchesCondition templatesMatches = (MatchesCondition) conditionDto0.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((Primitive) t).getValue().equals(CORONA_TEMPLATE)));
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
  public void getAllProjectsWithPagination() {
    setupDataForProjectsWithPagination();
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_COORDINATOR);
    Pageable pageable = PageRequest.of(0,100);
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    List<Project> projects = projectService.getProjects("approvedCoordinatorId", roles,
                    SearchCriteria.builder()
                                  .sort("DESC")
                                  .sortBy("name")
                                  .build(), pageable).getContent();
    Sort.Order sortOrder = Sort.Order.desc("name").ignoreCase();
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    Assert.assertEquals(Long.valueOf(1L), projects.get(0).getId());
    Assert.assertEquals("approvedCoordinatorId", specificationArgumentCaptor.getValue().getLoggedInUserId());
    Assert.assertEquals(roles, specificationArgumentCaptor.getValue().getRoles());
    Assert.assertNull(specificationArgumentCaptor.getValue().getFilter());
    Assert.assertEquals(sortOrder, specificationArgumentCaptor.getValue().getSortOrder());
  }

  @Test
  public void getAllProjectsWithPaginationAndSortByOrganization() {
    setupDataForProjectsWithPagination();
    Pageable pageable = PageRequest.of(0,100);
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    Page<Project> filteredProjects = projectService.getProjects("approvedCoordinatorId", List.of(STUDY_COORDINATOR),
            SearchCriteria.builder()
                    .sort("ASC")
                    .sortBy("organization")
                    .build(), pageable);
    Sort.Order sortOrder = Sort.Order.asc("organization").ignoreCase();
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    List<Project> projects = filteredProjects.getContent();
    ProjectSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(sortOrder, capturedInput.getSortOrder());
  }

  @Test
  public void getAllProjectsWithPaginationAndFilter() {
    setupDataForProjectsWithPagination();
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_COORDINATOR);
    roles.add(RESEARCHER);
    Pageable pageable = PageRequest.of(0,100);
    Map<String, String> filter = new HashMap<>();
    filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "OnE");
    filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.OWNED.name());
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    Set<String> owners = new HashSet<>();
    owners.add("approvedCoordinator");
    Mockito.when(userService.findUsersUUID(Mockito.eq("OnE"))).thenReturn(owners);
    Page<Project> filteredProjects = projectService.getProjects("approvedCoordinatorId", roles,
            SearchCriteria.builder()
                    .sort("ASC")
                    .sortBy("name")
                    .filter(filter)
                    .build(), pageable);
    Sort.Order sortOrder = Sort.Order.asc("name").ignoreCase();
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    List<Project> projects = filteredProjects.getContent();
    Assert.assertEquals(Long.valueOf(1L), projects.get(0).getId());
    ProjectSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(filter, capturedInput.getFilter());
    Assert.assertEquals("approvedCoordinatorId", capturedInput.getLoggedInUserId());
    Assert.assertEquals(roles, capturedInput.getRoles());
    Assert.assertEquals(owners, capturedInput.getOwnersUUID());
    Assert.assertEquals(sortOrder, capturedInput.getSortOrder());
  }

  @Test
  public void getAllProjectsWithPaginationAndSortByAuthor() {
    setupDataForProjectsWithPagination();
    when(userService.getOwner("approvedCoordinatorId")).thenReturn(User.builder().id("approvedCoordinatorId").firstName("AA Coordinator first name").build());
    Mockito.when(projectRepository.count()).thenReturn(50L);
    Pageable pageable = PageRequest.of(0,100);
    Page<Project> filteredProjects = projectService.getProjects("approvedCoordinatorId", List.of(STUDY_COORDINATOR),
            SearchCriteria.builder()
                    .sort("DESC")
                    .sortBy("author")
                    .build(), pageable);
    Pageable authorPageable = PageRequest.of(0,50);
    Mockito.verify(projectRepository, times(1)).findProjects(Mockito.any(ProjectSpecification.class), Mockito.eq(authorPageable));
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
    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
            .thenReturn(UserDetails.builder().build());
    projectService.getProjects("approvedCoordinatorId", List.of(STUDY_COORDINATOR), searchCriteria, pageable);
    verify(projectRepository, never());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingSortFieldWhenGetProjectsWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sortBy("ASC")
            .build();
    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
            .thenReturn(UserDetails.builder().build());
    projectService.getProjects("approvedCoordinatorId", List.of(STUDY_COORDINATOR), searchCriteria, pageable);
    verify(projectRepository, never());
  }

  @Test
  public void getAllProjectsWithStudyApprover() {
    List<String> roles = new ArrayList<>();
    roles.add(STUDY_APPROVER);
    setupDataForProjectsWithPagination();
    when(userDetailsService.checkIsUserApproved("approverId"))
            .thenReturn(UserDetails.builder()
                    .userId("approverId")
                    .approved(true)
                    .organization(Organization.builder().id(1L).build())
                    .build());
    Pageable pageable = PageRequest.of(0,50);
    Map<String, String> filter = new HashMap<>();
    filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ORGANIZATION.name());
    ArgumentCaptor<ProjectSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(ProjectSpecification.class);
    projectService.getProjects("approverId", roles,
            SearchCriteria.builder()
                    .sort("DESC")
                    .sortBy("status")
                    .filter(filter)
                    .build(), pageable);
    Sort.Order sortOrder = Sort.Order.desc("status").ignoreCase();
    Mockito.verify(projectRepository, times(1)).findProjects(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    ProjectSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(filter, capturedInput.getFilter());
    Assert.assertEquals("approverId", capturedInput.getLoggedInUserId());
    Assert.assertEquals(roles, capturedInput.getRoles());
    Assert.assertEquals(sortOrder, capturedInput.getSortOrder());
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
    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
            .thenReturn(coordinator);
    Project pr1 = Project.builder().id(1L)
            .name("project name one")
            .status(ProjectStatus.APPROVED)
            .coordinator(coordinator)
            .build();
    Project pr2 = Project.builder().id(2L)
            .name("project name two")
            .status(PUBLISHED)
            .coordinator(anotherCoordinator)
            .build();
    Project pr3 = Project.builder().id(3L)
            .name("project name blaaa")
            .status(PUBLISHED)
            .coordinator(anotherCoordinator)
            .build();
    when(userService.getOwner("anotherCoordinatorId")).thenReturn(User.builder().id("anotherCoordinatorId").firstName("Coordinator first name").build());
    Mockito.when(projectRepository.findProjects(Mockito.any(ProjectSpecification.class), Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(pr1, pr2, pr3)));
  }

  @Test
  public void shouldHandleMissingProject() {
    Optional<Project> project = projectService.getProjectById("approvedCoordinatorId", 19L);

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
    User userOne = User.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@vitagorup.de")
            .approved(true)
            .build();
    Mockito.when(userService.getByRole(STUDY_APPROVER)).thenReturn(Set.of(userOne));

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
    public void shouldCreateMultipartProject() {
        ProjectDto newStudy =
                ProjectDto.builder()
                        .name("new project")
                        .financed(false)
                        .status(ProjectStatus.DRAFT)
                        .build();

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile };

        projectService.createMultipartProject(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR), multipartFiles);
        verify(projectRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateMultipartProject() {
        Project projectToEdit =
                Project.builder()
                        .name("Project")
                        .status(ProjectStatus.PENDING)
                        .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
                        .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

        ProjectDto projectDto =
                ProjectDto.builder().name("Project is edited").status(ProjectStatus.DRAFT).build();

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile };

        projectService.updateMultipartProject(
                projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR), multipartFiles);
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
    public void shouldHandleExecuteManagerProjectWithEmptyTemplates() {
        executeManagerProjectWithoutTemplates(Collections.EMPTY_LIST);
    }

    @Test
    public void shouldHandleExecuteManagerProjectWithNullTemplates() {
        executeManagerProjectWithoutTemplates(null);
    }

    private void executeManagerProjectWithoutTemplates(List<String> templates) {
        CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();
        UserDetails userDetails =
                UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();
        String result =
                projectService.executeManagerProject(
                        cohortDto, templates, userDetails.getUserId());
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

    Project pr1 = new Project(1L, "project one", OffsetDateTime.now(),
            UserDetails.builder()
            .userId("approvedCoordinatorId")
            .approved(true)
            .organization(Organization.builder()
                    .name("some organization")
                    .id(3L).build())
            .build());

    Project pr2 = new Project(1L, "project two", OffsetDateTime.now(),
            UserDetails.builder()
            .userId("approvedCoordinatorId")
            .approved(true)
            .organization(Organization.builder()
                    .name("some organization")
                    .id(3L).build())
            .build());
    Mockito.when(projectRepository.findByStatusInOrderByCreateDateDesc(Arrays.asList(ProjectStatus.APPROVED,
                 PUBLISHED, CLOSED), PageRequest.of(0, 10)))
                .thenReturn(Arrays.asList(pr1,pr2));
    projectService.getLatestProjectsInfo(10, roles);
    verify(projectRepository, times(1)).findByStatusInOrderByCreateDateDesc(Arrays.asList(ProjectStatus.APPROVED,
            PUBLISHED, CLOSED), PageRequest.of(0, 10));
  }

  @Test
  public void shouldReturnNoProjectsWhenCounterLessOneTest() {
    List<ProjectInfoDto> projects = projectService.getLatestProjectsInfo(0, List.of(STUDY_COORDINATOR));
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
    projectService.deleteProject(9L, userId, List.of(STUDY_COORDINATOR));
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

    projectService.deleteProject(1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(projectRepository, times(0)).deleteById(1L);
  }

  @Test
  public void archiveProjectTest() {
    when(projectRepository.findById(5L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(5L)
                                    .status(CLOSED)
                                    .coordinator(UserDetails.builder().userId("approvedCoordinatorId").approved(true).build())
                                    .build()));
    projectService.archiveProject(5L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
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
    public void retrieveDataCustomConfigurationTest() {
        projectService.retrieveData("Select e from EHR e", 2L,"approvedCoordinatorId", false);
        verify(cohortService, times(1)).executeCohort(Mockito.eq(2L), Mockito.eq(false));
    }

    @Test
    public void retrieveDataForProjectWithoutTemplatesTest() {
        try {
            projectService.retrieveData("select * from dummy", 22L, "approvedCoordinatorId", true);
        } catch (BadRequestException be) {
            Assert.assertTrue(true);
            Assert.assertEquals(PROJECT_TEMPLATES_CANNOT_BE_NULL, be.getParamValue());
            verify(cohortService, Mockito.never()).executeCohort(Mockito.anyLong(), Mockito.eq(false));
        }
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
    AqlQuery aqlDto = AqlQueryParser.parse(QUERY_5);
    when(templateService.createSelectCompositionQuery(Mockito.any())).thenReturn(aqlDto);
    projectService.getExportResponseBody("select * from dummy", 2L, "approvedCoordinatorId", ExportType.json, true);
    Mockito.verify(cohortService, times(1)).executeCohort(Mockito.any(Cohort.class), Mockito.eq(false));
  }

    @Test
    public void getExportResponseBodyAsCSVTest() {
        projectService.getExportResponseBody(QUERY_5, 2L, "approvedCoordinatorId", ExportType.csv, false);
        Mockito.verify(cohortService, times(1)).executeCohort(Mockito.eq(2L), Mockito.eq(false));
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
    projectService.streamResponseAsZip(List.of(response), "testFile", out);

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry expectedFile = zipInputStream.getNextEntry();
    Assert.assertEquals("testFile_response-one.csv", expectedFile.getName());
  }

  @Test
  public void getManagerExportResponseBodyTest() {
    CohortDto cohortDto = CohortDto.builder()
            .name("alter cohort")
            .projectId(2L).build();
    projectService.getManagerExportResponseBody(cohortDto, List.of("Alter"), "approvedCoordinatorId", ExportType.json);
    Mockito.verify(cohortService, Mockito.times(1)).toCohort(Mockito.any(CohortDto.class));
  }

  @Test
  public void existsTest() {
    projectService.exists(5L);
    Mockito.verify(projectRepository, Mockito.times(1)).existsById(5L);
  }

    @Test
    public void shouldAllowDeleteAttachmentWhenProjectWithDraftStatus() {
        Project projectToEdit =
                Project.builder()
                        .name("Project")
                        .status(ProjectStatus.DRAFT)
                        .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
                        .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

        ProjectDto projectDto =
                ProjectDto.builder()
                        .name("Project is edited")
                        .attachmentsToBeDeleted(Set.of(1L, 2L))
                        .status(DRAFT).build();
        projectService.updateProject(
                projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    }
    @Test
    public void shouldAllowDeleteAttachmentWhenProjectWithReviewStatus() {
        Project projectToEdit =
                Project.builder()
                        .name("Project")
                        .status(PENDING)
                        .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
                        .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

        ProjectDto projectDto =
                ProjectDto.builder()
                        .name("Project is edited")
                        .attachmentsToBeDeleted(Set.of(1L, 2L))
                        .status(REVIEWING).build();
        projectService.updateProject(
                projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
    }

    @Test
    public void shouldRejectDeleteAttachmentsWhenProjectPendingTest() {
        Project projectToEdit =
                Project.builder()
                        .name("Project")
                        .status(PENDING)
                        .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
                        .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectToEdit));

        ProjectDto projectDto =
                ProjectDto.builder()
                        .name("Project is edited")
                        .status(PENDING)
                        .attachmentsToBeDeleted(Set.of(3L))
                        .build();

        Exception exception =
                assertThrows(
                        ForbiddenException.class,
                        () ->
                                projectService.updateProject(
                                        projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

        String expectedMessage = String.format(CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS, PENDING);
        assertThat(exception.getMessage(), is(expectedMessage));
    }

  @Before
  public void setup() {
    when(userDetailsService.getUserDetailsById("researcher1"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("researcher1").approved(true).build()));
    UserDetails ownerCoordinator  = UserDetails.builder()
            .userId("ownerCoordinatorId")
            .approved(true).build();
    User researcher2 = User.builder()
            .id("researcher2")
            .firstName("f2")
            .lastName("l2")
            .email("em2@vitagroup.ag")
            .build();
      UserDetails researcher = UserDetails.builder()
              .userId("researcher2")
              .approved(true)
              .build();
    when(userService.getUserById("researcher2", false)).thenReturn(researcher2);

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

    projectOne = Project.builder()
            .id(1L)
            .status(PUBLISHED)
            .cohort(Cohort.builder().id(2L).build())
            .researchers(List.of(approvedCoordinator))
            .build();
    when(projectRepository.findById(1L))
        .thenReturn(Optional.of(projectOne));
    projectDtoOne = ProjectDto.builder()
            .id(1L)
            .status(PUBLISHED)
            .build();
    when(projectMapper.convertToDto(projectOne)).thenReturn(projectDtoOne);
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
                                    .coordinator(ownerCoordinator)
                                    .researchers(List.of(researcher))
                                    .build()));

    when(projectRepository.findById(7L))
            .thenReturn(
                    Optional.of(
                            Project.builder()
                                    .id(7L)
                                    .cohort(Cohort.builder().id(5L).build())
                                    .status(PUBLISHED)
                                    .coordinator(ownerCoordinator)
                                    .researchers(List.of(researcher))
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
                                    .coordinator(ownerCoordinator)
                                    .researchers(List.of(researcher))
                                    .build()));

      when(cohortService.executeCohort(2L, false)).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
      when(cohortService.executeCohort(4L, false)).thenReturn(Set.of(EHR_ID_3));
      when(cohortService.executeCohort(5L, true)).thenReturn(Set.of(EHR_ID_2, EHR_ID_3));
      when(cohortService.executeCohort(any(), any())).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
      when(privacyProperties.getMinHits()).thenReturn(0);
      when(consentProperties.getAllowUsageOutsideEuOid()).thenReturn("1937.777.24.5.1.37");

      //project without template
      when(projectRepository.findById(22L))
              .thenReturn(
                      Optional.of(
                              Project.builder()
                                      .id(22L)
                                      .status(PUBLISHED)
                                      .cohort(Cohort.builder().id(2L).build())
                                      .researchers(List.of(approvedCoordinator))
                                      .build()));

      // project used outside eu
      when(projectRepository.findById(33L))
              .thenReturn(
                      Optional.of(
                              Project.builder()
                                      .id(33L)
                                      .status(PUBLISHED)
                                      .cohort(Cohort.builder().id(5L).build())
                                      .researchers(List.of(approvedCoordinator))
                                      .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
                                      .usedOutsideEu(true)
                                      .build()));
  }
}
