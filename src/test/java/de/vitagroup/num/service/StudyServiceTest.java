package de.vitagroup.num.service;

import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StudyServiceTest {

  @Mock private StudyRepository studyRepository;

  @Mock private UserDetailsRepository userDetailsRepository;

  @InjectMocks private StudyService studyService;

  @Before
  public void setup() {
    UserDetails notApprovedCoordinator =
        UserDetails.builder().userId("notApprovedCoordinatorId").approved(false).build();

    UserDetails approvedCoordinator =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    when(userDetailsRepository.findByUserId("approvedCoordinatorId"))
        .thenReturn(Optional.of(approvedCoordinator));

    when(userDetailsRepository.findByUserId("notApprovedCoordinatorId"))
        .thenReturn(Optional.of(notApprovedCoordinator));

    when(userDetailsRepository.findByUserId("nonExistingCoordinatorId"))
        .thenReturn(Optional.empty());
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingCoordinator() {
    StudyDto study =
        StudyDto.builder()
            .name("Study")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetailsDto.builder().userId("someCoordinatorId").build())
            .build();

    studyService.createStudy(study, "nonExistingCoordinatorId", List.of());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedCoordinator() {
    StudyDto studyDto =
        StudyDto.builder()
            .name("Study")
            .coordinator(UserDetailsDto.builder().userId("someCoordinatorId").build())
            .build();

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
  public void shouldCallRepoWhenRetrievingAllStudies() {
    studyService.getAllStudies();
    verify(studyRepository, times(1)).findAll();
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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.PUBLISHED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.CLOSED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
        StudyDto.builder()
            .name("Study is edited")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
            .build();

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
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
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
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
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
            .status(StudyStatus.CHANGE_REQUEST)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
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
            .status(StudyStatus.DENIED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
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
            .status(StudyStatus.DENIED)
            .coordinator(UserDetailsDto.builder().userId("approvedCoordinatorId").build())
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
    StudyDto newStudy = StudyDto.builder().name("new study").status(StudyStatus.CLOSED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.createStudy(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid study status: CLOSED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialApprovedStudyStatus() {
    StudyDto newStudy = StudyDto.builder().name("new study").status(StudyStatus.APPROVED).build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.createStudy(newStudy, "approvedCoordinatorId", List.of()));

    String expectedMessage = "Invalid study status: APPROVED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowInitialDraftStudyStatus() {
    StudyDto newStudy = StudyDto.builder().name("new study").status(StudyStatus.DRAFT).build();
    studyService.createStudy(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }

  @Test
  public void shouldAllowInitialPendingStudyStatus() {
    StudyDto newStudy = StudyDto.builder().name("new study").status(StudyStatus.PENDING).build();
    studyService.createStudy(newStudy, "approvedCoordinatorId", List.of(STUDY_COORDINATOR));
    verify(studyRepository, times(1)).save(any());
  }
}
