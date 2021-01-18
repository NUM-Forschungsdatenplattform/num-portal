package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
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
    Study study =
        Study.builder()
            .name("Study")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetails.builder().userId("someCoordinatorId").build())
            .build();

    studyService.createStudy(study, "nonExistingCoordinatorId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedCoordinator() {
    Study study =
        Study.builder()
            .name("Study")
            .coordinator(UserDetails.builder().userId("someCoordinatorId").build())
            .build();

    studyService.createStudy(study, "notApprovedCoordinatorId");
  }

  @Test
  public void shouldGetAllWhenSearchingStudiesWithoutCoordinator() {
    studyService.searchStudies(null);
    verify(studyRepository, times(1)).findAll();
  }

  @Test
  public void shouldFilterWhenSearchingStudiesWithCoordinator() {
    studyService.searchStudies("coordinatorId");

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId"));

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.PUBLISHED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId"));

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.CLOSED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId"));

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId"));

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId"));

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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
  }

  @Test
  public void shouldAllowStudyPendingToReviewingTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.PENDING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
  }

  @Test
  public void shouldAllowStudyReviewingToApprovedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.APPROVED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
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

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.CHANGE_REQUEST)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
  }

  @Test
  public void shouldAllowStudyReviewingToDeniedTransition() {
    Study studyToEdit =
        Study.builder()
            .name("Study")
            .status(StudyStatus.REVIEWING)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    when(studyRepository.findById(1L)).thenReturn(Optional.of(studyToEdit));

    Study newStudy =
        Study.builder()
            .name("Study is edited")
            .status(StudyStatus.DENIED)
            .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
            .build();

    studyService.updateStudy(newStudy, 1L, "approvedCoordinatorId");
  }

  @Test
  public void shouldRejectInitialApprovedStudyStatus() {
    Study study = new Study();

    Exception exception =
        assertThrows(BadRequestException.class, () -> study.setStatus(StudyStatus.APPROVED));

    String expectedMessage = "Invalid study status: APPROVED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldRejectInitialClosedStudyStatus() {
    Study study = new Study();

    Exception exception =
        assertThrows(BadRequestException.class, () -> study.setStatus(StudyStatus.CLOSED));

    String expectedMessage = "Invalid study status: CLOSED";
    assertThat(exception.getMessage(), is(expectedMessage));
  }

  @Test
  public void shouldAllowInitialDraftStudyStatus() {
    Study study = new Study();
    study.setStatus(StudyStatus.DRAFT);
  }
}
