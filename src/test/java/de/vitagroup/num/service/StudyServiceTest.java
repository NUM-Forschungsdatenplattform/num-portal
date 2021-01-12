package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
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
}
