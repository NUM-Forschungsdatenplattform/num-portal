package de.vitagroup.num.service;

import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorSymbol;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.junit.Before;
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

  @Mock
  private StudyRepository studyRepository;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CohortService cohortService;

  @Mock
  private EhrBaseService ehrBaseService;

  @InjectMocks
  private StudyService studyService;

  @Captor
  ArgumentCaptor<String> stringArgumentCaptor;

  @Before
  public void setup() {
    UserDetails notApprovedCoordinator =
        UserDetails.builder().userId("notApprovedCoordinatorId").approved(false).build();

    UserDetails approvedCoordinator =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    when(userDetailsService.validateAndReturnUserDetails("approvedCoordinatorId"))
        .thenReturn(approvedCoordinator);

    when(userDetailsService.validateAndReturnUserDetails("notApprovedCoordinatorId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(userDetailsService.validateAndReturnUserDetails("nonExistingCoordinatorId"))
        .thenThrow(new SystemException("User not found"));

    when(studyRepository.findById(1L)).thenReturn(Optional
        .of(Study.builder().id(1L).cohort(Cohort.builder().id(2L).build())
            .researchers(List.of(approvedCoordinator)).build()));
  }

  @Test
  public void shouldCorrectlyRestrictQuery() {
    when(cohortService.executeCohort(2L)).thenReturn(
        Set.of("f4da8646-8e36-4d9d-869c-af9dce5935c7", "61861e76-1606-48c9-adcf-49ebbb2c6bbd"));

    String query = "Select o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Systolic__magnitude, e/ehr_id/value as ehr_id from EHR e contains OBSERVATION o0[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1] where (o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude >= $magnitude and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude < 1.1)";
    studyService.executeAql(query, 1L, "approvedCoordinatorId");

    Mockito.verify(ehrBaseService).executeRawQuery(stringArgumentCaptor.capture());
    String restrictedQuery = stringArgumentCaptor.getValue();

    AqlDto newAqlDto = new AqlToDtoParser().parse(restrictedQuery);
    assertThat(newAqlDto.getWhere(), notNullValue());
    ConditionLogicalOperatorDto conditionDto = (ConditionLogicalOperatorDto)newAqlDto.getWhere();
    assertThat(conditionDto.getSymbol(), is(ConditionLogicalOperatorSymbol.AND));
    assertThat(conditionDto.getValues().size(), is(3));

    conditionDto.getValues().stream().anyMatch(conditionDto1 -> conditionDto1 instanceof MatchesOperatorDto);

    conditionDto.getValues().forEach(condition -> {
      if(condition instanceof MatchesOperatorDto){
        assertThat(((MatchesOperatorDto) condition).getValues().size(), is(2));

        assertTrue(((MatchesOperatorDto) condition).getValues().stream().anyMatch(value1 -> ((SimpleValue)value1).getValue().equals("61861e76-1606-48c9-adcf-49ebbb2c6bbd")));
        assertTrue(((MatchesOperatorDto) condition).getValues().stream().anyMatch(value1 -> ((SimpleValue)value1).getValue().equals("f4da8646-8e36-4d9d-869c-af9dce5935c7")));
      }
    });
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
    Optional<Study> study = studyService.getStudyById(1L);

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

  @Test
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

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                studyService.updateStudy(
                    studyDto, 1L, "approvedCoordinatorId", List.of(STUDY_APPROVER)));

    String expectedMessage = "Study status transition from CLOSED to APPROVED not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
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

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                studyService.updateStudy(
                    studyDto,
                    1L,
                    "approvedCoordinatorId",
                    List.of(STUDY_COORDINATOR, STUDY_APPROVER)));

    String expectedMessage = "Study status transition from CLOSED to DRAFT not allowed";
    assertThat(exception.getMessage(), is(expectedMessage));
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
}
