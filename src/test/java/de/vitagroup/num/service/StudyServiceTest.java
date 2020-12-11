package de.vitagroup.num.service;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.when;

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

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingCoordinator() {

    Study study =
        Study.builder()
            .name("Study")
            .coordinator(UserDetails.builder().userId("someCoordinatorId").build())
            .build();

    studyService.createStudy(study, "nonExistingCoordinatorId");
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleNotApprovedCoordinator() {

    Study study =
        Study.builder()
            .name("Study")
            .coordinator(UserDetails.builder().userId("someCoordinatorId").build())
            .build();

    studyService.createStudy(study, "notApprovedCoordinatorId");
  }
}
