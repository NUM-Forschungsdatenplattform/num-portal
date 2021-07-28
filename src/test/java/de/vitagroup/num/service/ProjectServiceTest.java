package de.vitagroup.num.service;

import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.ehrbase.ResponseFilter;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  @Mock private UserService userService;

  @Spy private ResponseFilter responseFilter;

  @Spy private ProjectPolicyService projectPolicyService;

  @Spy private ObjectMapper mapper;

  @InjectMocks private ProjectService projectService;

  @Spy private AqlEditorAqlService aqlEditorAqlService;

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

    verify(projectRepository, times(1)).findByCoordinatorUserId("coordinatorId");
    verify(projectRepository, times(0)).findAll();
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

    String expectedMessage = "Project status transition from DRAFT to APPROVED not allowed";
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
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.PUBLISHED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from DRAFT to PUBLISHED not allowed";
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
        ProjectDto.builder().name("Project is edited").status(ProjectStatus.CLOSED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                projectService.updateProject(
                    projectDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Project status transition from DRAFT to CLOSED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectStudyClosedToApprovedTransition() {
    Project projectToEdit =
        Project.builder()
            .name("Project")
            .status(ProjectStatus.CLOSED)
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
            .status(ProjectStatus.CLOSED)
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

    String expectedMessage = "Project status transition from PENDING to REVIEWING not allowed";
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

    String expectedMessage = "Project status transition from REVIEWING to APPROVED not allowed";
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

    String expectedMessage = "Project status transition from REVIEWING to DENIED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialClosedStudyStatus() {
    ProjectDto newStudy =
        ProjectDto.builder()
            .name("new project")
            .financed(false)
            .status(ProjectStatus.CLOSED)
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
                    .status(ProjectStatus.CLOSED)
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    ProjectDto existingStudy =
        ProjectDto.builder()
            .id(1L)
            .name("existing Project")
            .financed(false)
            .status(ProjectStatus.CLOSED)
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
            QUERY_BASIC, cohortDto, List.of(CORONA_TEMPLATE), userDetails.getUserId());

    assertThat(result, is("[]"));
  }

  @Before
  public void setup() {
    UserDetails approvedCoordinator =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    User approvedUser = User.builder().id("approvedCoordinatorId").approved(true).build();

    when(userService.getUserById("approvedCoordinatorId", false)).thenReturn(approvedUser);

    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
        .thenReturn(approvedCoordinator);

    when(userDetailsService.checkIsUserApproved("notApprovedCoordinatorId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(userDetailsService.checkIsUserApproved("nonExistingCoordinatorId"))
        .thenThrow(new SystemException("User not found"));

    when(projectRepository.findById(3L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(3L)
                    .status(ProjectStatus.PUBLISHED)
                    .researchers(List.of(approvedCoordinator))
                    .build()));

    when(projectRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(1L)
                    .status(ProjectStatus.PUBLISHED)
                    .cohort(Cohort.builder().id(2L).build())
                    .researchers(List.of(approvedCoordinator))
                    .build()));

    when(projectRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .id(2L)
                    .status(ProjectStatus.PUBLISHED)
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
                    .status(ProjectStatus.PUBLISHED)
                    .cohort(Cohort.builder().id(4L).build())
                    .researchers(List.of(approvedCoordinator))
                    .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
                    .build()));

    when(cohortService.executeCohort(2L, false)).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
    when(cohortService.executeCohort(4L, false)).thenReturn(Set.of(EHR_ID_3));
    when(cohortService.executeCohort(any())).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
  }
}
