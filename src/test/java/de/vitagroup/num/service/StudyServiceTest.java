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

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.email.ZarsService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorSymbol;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StudyServiceTest {

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

  @Captor ArgumentCaptor<String> stringArgumentCaptor;
  @Mock private StudyRepository studyRepository;
  @Mock private UserDetailsService userDetailsService;
  @Mock private CohortService cohortService;
  @Mock private EhrBaseService ehrBaseService;
  @Mock private AtnaService atnaService;
  @Mock private ZarsService zarsService;
  @InjectMocks private StudyService studyService;

  @Ignore(
      value =
          "This test should pass when https://github.com/ehrbase/openEHR_SDK/issues/203 is fixed")
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

    studyService.executeAql(QUERY_5, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    AqlDto parsedRestrictedQuery = new AqlToDtoParser().parse(restrictedQuery);
    assertThat(parsedRestrictedQuery, notNullValue());
    assertThat(parsedRestrictedQuery.getWhere(), notNullValue());

    assertThat(parsedRestrictedQuery.getWhere(), notNullValue());
  }

  @Test
  public void shouldHandleQuery3() {
    studyService.executeAql(QUERY_3, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    AqlDto dto = new AqlToDtoParser().parse(restrictedQuery);

    assertThat(dto, notNullValue());
    assertThat(dto.getWhere(), notNullValue());

    ConditionLogicalOperatorDto conditionDto = (ConditionLogicalOperatorDto) dto.getWhere();
    assertThat(conditionDto.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto.getValues().size(), is(2));

    conditionDto.getValues().stream()
        .anyMatch(
            conditionDto1 ->
                conditionDto1 instanceof MatchesOperatorDto); // TODO: should test the return value

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((SimpleValue) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test
  public void shouldHandleQuery4() {
    studyService.executeAql(QUERY_4, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    AqlDto dto = new AqlToDtoParser().parse(restrictedQuery);

    assertThat(dto, notNullValue());
    assertThat(dto.getWhere(), notNullValue());

    ConditionLogicalOperatorDto conditionDto = (ConditionLogicalOperatorDto) dto.getWhere();
    assertThat(conditionDto.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto.getValues().size(), is(2));

    conditionDto.getValues().stream()
        .anyMatch(
            conditionDto1 ->
                conditionDto1 instanceof MatchesOperatorDto); // TODO: should test the return value

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto.getValues().get(1);
    assertThat(templatesMatches.getValues().size(), is(1));
    assertTrue(
        templatesMatches.getValues().stream()
            .anyMatch(t -> ((SimpleValue) t).getValue().equals(CORONA_TEMPLATE)));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleStudyWithoutTemplates() {
    studyService.executeAql(QUERY_1, 1L, "approvedCoordinatorId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleStudyWithoutCohort() {
    studyService.executeAql(QUERY_1, 3L, "approvedCoordinatorId");
  }

  @Ignore(
      value =
          "this test should pass when https://github.com/ehrbase/openEHR_SDK/issues/176 is fixed")
  @Test
  public void shouldCorrectlyRestrictQueryWithContainsAndNoComposition() {
    studyService.executeAql(QUERY_2, 4L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

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

  @Ignore(
      value =
          "this test should pass when https://github.com/ehrbase/openEHR_SDK/issues/176 is fixed")
  @Test
  public void shouldCorrectlyRestrictBasicQuery() {
    studyService.executeAql(QUERY_BASIC, 2L, "approvedCoordinatorId");
    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    new AqlToDtoParser().parse(restrictedQuery);
  }

  @Test
  public void shouldCorrectlyRestrictQuery() {
    studyService.executeAql(QUERY_1, 2L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    AqlDto newAqlDto = new AqlToDtoParser().parse(restrictedQuery);

    assertThat(newAqlDto.getWhere(), notNullValue());
    ConditionLogicalOperatorDto conditionDto = (ConditionLogicalOperatorDto) newAqlDto.getWhere();
    assertThat(conditionDto.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto.getValues().size(), is(2));

    conditionDto.getValues().stream()
        .anyMatch(
            conditionDto1 ->
                conditionDto1 instanceof MatchesOperatorDto); // TODO: should test the return value

    MatchesOperatorDto ehrMatches = (MatchesOperatorDto) conditionDto.getValues().get(0);
    assertThat(ehrMatches.getValues().size(), is(2));

    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_1)));
    assertTrue(
        ehrMatches.getValues().stream()
            .anyMatch(e -> ((SimpleValue) e).getValue().equals(EHR_ID_2)));

    MatchesOperatorDto templatesMatches = (MatchesOperatorDto) conditionDto.getValues().get(1);
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
      studyService.executeAql(query, 1L, "notApprovedCoordinatorId");
    } catch (Exception e) {
      exception = e;
    }

    Mockito.verify(atnaService, times(1))
        .logDataExport(eq("notApprovedCoordinatorId"), eq(1L), eq(null), eq(false));

    assertTrue(exception instanceof ForbiddenException);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingCoordinator() {
    StudyDto study = StudyDto.builder().name("Study").status(StudyStatus.APPROVED).build();

    studyService.createStudy(study, "nonExistingCoordinatorId", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedCoordinator() {
    StudyDto studyDto = StudyDto.builder().name("Study").build();

    studyService.createStudy(studyDto, "notApprovedCoordinatorId", List.of());
  }

  @Test
  public void shouldFilterWhenSearchingStudiesWithCoordinator() {
    List<String> roles = new ArrayList<>();
    roles.add(Roles.STUDY_COORDINATOR);
    studyService.getStudies("coordinatorId", roles);

    verify(studyRepository, times(1)).findByCoordinatorUserId("coordinatorId");
    verify(studyRepository, times(0)).findAll();
  }

  @Test
  public void shouldHandleMissingStudy() {
    Optional<Study> study = studyService.getStudyById(19L);

    assertThat(study, notNullValue());
    assertThat(study.isEmpty(), is(true));
  }

  @Test
  public void shouldRejectStudyDraftToApprovedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.APPROVED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                studyService.updateStudy(
                    studyDto,
                    1L,
                    "approvedCoordinatorId",
                    List.of(STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER)));

    String expectedMessage = "Study status transition from DRAFT to APPROVED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectStudyDraftToPublishedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.PUBLISHED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Study status transition from DRAFT to PUBLISHED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectStudyDraftToClosedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.CLOSED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Study status transition from DRAFT to CLOSED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectStudyClosedToApprovedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.CLOSED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.APPROVED).build();

    studyService.updateStudy(
        studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectStudyClosedToDraftTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.CLOSED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.DRAFT).build();

    studyService.updateStudy(
        studyDto,
        1L,
        "approvedCoordinatorId",
        List.of(STUDY_COORDINATOR, STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyDraftToPendingTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.PENDING).build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldAllowStudyPendingToDraftTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.DRAFT).build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldAllowStudyPendingToReviewingTransitionToApprover() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.REVIEWING).build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldDenyStudyPendingToReviewingTransitionToCoordinator() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder().name("Study is edited").status(StudyStatus.REVIEWING).build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Study status transition from PENDING to REVIEWING not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowStudyReviewingToApprovedTransitionToApprover() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .financed(false)
            .build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyReviewingToApprovedTransitionToCoordinator() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder()
            .name("Study is edited")
            .financed(false)
            .status(StudyStatus.APPROVED)
            .build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR)));

    String expectedMessage = "Study status transition from REVIEWING to APPROVED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowStudyReviewingToChangeRequestTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder()
            .name("Study is edited")
            .financed(false)
            .status(StudyStatus.CHANGE_REQUEST)
            .build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldAllowStudyReviewingToDeniedTransitionToApprover() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder()
            .name("Study is edited")
            .financed(false)
            .status(StudyStatus.DENIED)
            .build();

    studyService.updateStudy(studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER));
  }

  @Test
  public void shouldDenyStudyReviewingToDeniedTransitionToCoordinatorAndResearcher() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    StudyDto studyDto =
        StudyDto.builder()
            .name("Study is edited")
            .financed(false)
            .status(StudyStatus.DENIED)
            .build();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR, RESEARCHER)));

    String expectedMessage = "Study status transition from REVIEWING to DENIED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialClosedStudyStatus() {
    StudyDto newStudy =
        StudyDto.builder().name("new study").financed(false).status(StudyStatus.CLOSED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.createStudy(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid study status: CLOSED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialApprovedStudyStatus() {
    StudyDto newStudy =
        StudyDto.builder().name("new study").financed(false).status(StudyStatus.APPROVED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.createStudy(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid study status: APPROVED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowInitialDraftStudyStatus() {
    StudyDto newStudy =
        StudyDto.builder().name("new study").financed(false).status(StudyStatus.DRAFT).build();
    studyService.createStudy(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }

  @Test
  public void shouldAllowInitialPendingStudyStatus() {
    StudyDto newStudy =
        StudyDto.builder().name("new study").financed(false).status(StudyStatus.PENDING).build();
    studyService.createStudy(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }

  @Test
  public void shouldAllowEditingOwnedStudy() {
    when(studyRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(1L)
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    StudyDto existingStudy =
        StudyDto.builder()
            .id(1L)
            .name("existing study")
            .financed(false)
            .status(StudyStatus.PENDING)
            .build();
    studyService.updateStudy(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectEditingNotOwnedStudy() {
    when(studyRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(1L)
                    .coordinator(new UserDetails("ownerCoordinatorId", null, true))
                    .build()));

    StudyDto existingStudy =
        StudyDto.builder()
            .id(1L)
            .name("existing study")
            .financed(false)
            .status(StudyStatus.PENDING)
            .build();
    studyService.updateStudy(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
  }

  @Test
  public void shouldOnlyAllowEditingOfResearchersAfterApprovedState() {
    when(studyRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(1L)
                    .name("oldName")
                    .status(StudyStatus.APPROVED)
                    .researchers(
                        Collections.singletonList(
                            UserDetails.builder().userId("2").approved(true).build()))
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    when(userDetailsService.getUserDetailsById("1"))
        .thenReturn(Optional.of(UserDetails.builder().userId("1").approved(true).build()));

    StudyDto existingStudy =
        StudyDto.builder()
            .id(1L)
            .name("existing study")
            .status(StudyStatus.APPROVED)
            .researchers(
                Collections.singletonList(
                    UserDetailsDto.builder().userId("1").approved(true).build()))
            .financed(false)
            .build();
    Study returnedStudy =
        studyService.updateStudy(
            existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));

    assertThat(returnedStudy.getName(), is("oldName"));
    assertThat(returnedStudy.getResearchers().get(0).getUserId(), is("1"));

    verify(studyRepository, times(1)).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldRejectEditingClosedStudies() {
    when(studyRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(1L)
                    .status(StudyStatus.CLOSED)
                    .coordinator(new UserDetails("approvedCoordinatorId", null, true))
                    .build()));

    StudyDto existingStudy =
        StudyDto.builder()
            .id(1L)
            .name("existing study")
            .financed(false)
            .status(StudyStatus.CLOSED)
            .build();
    studyService.updateStudy(
        existingStudy, 1L, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }

  @Before
  public void setup() {
    UserDetails notApprovedCoordinator =
        UserDetails.builder().userId("notApprovedCoordinatorId").approved(false).build();

    UserDetails approvedCoordinator =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    when(userDetailsService.checkIsUserApproved("approvedCoordinatorId"))
        .thenReturn(approvedCoordinator);

    when(userDetailsService.checkIsUserApproved("notApprovedCoordinatorId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(userDetailsService.checkIsUserApproved("nonExistingCoordinatorId"))
        .thenThrow(new SystemException("User not found"));

    when(studyRepository.findById(3L))
        .thenReturn(
            Optional.of(Study.builder().id(3L).researchers(List.of(approvedCoordinator)).build()));

    when(studyRepository.findById(1L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(1L)
                    .cohort(Cohort.builder().id(2L).build())
                    .researchers(List.of(approvedCoordinator))
                    .build()));

    when(studyRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Study.builder()
                    .id(2L)
                    .cohort(Cohort.builder().id(2L).build())
                    .researchers(List.of(approvedCoordinator))
                    .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
                    .build()));

    when(studyRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Study study = invocation.getArgument(0, Study.class);
              study.setId(1L);
              return study;
            });

    //    when(studyRepository.findById(4L))
    //        .thenReturn(
    //            Optional.of(
    //                Study.builder()
    //                    .id(4L)
    //                    .cohort(Cohort.builder().id(4L).build())
    //                    .researchers(List.of(approvedCoordinator))
    //                    .templates(Map.of(CORONA_TEMPLATE, CORONA_TEMPLATE))
    //                    .build()));

    when(cohortService.executeCohort(2L)).thenReturn(Set.of(EHR_ID_1, EHR_ID_2));
    //    when(cohortService.executeCohort(4L)).thenReturn(Set.of(EHR_ID_3));
  }
}
