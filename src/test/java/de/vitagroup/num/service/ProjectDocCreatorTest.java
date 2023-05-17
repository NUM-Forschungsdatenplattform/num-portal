package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.exception.SystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CAN_T_FIND_THE_COHORT_BY_ID;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDocCreatorTest {

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks
  private ProjectDocCreator projectDocCreator;

  @Mock private CohortRepository cohortRepository;

  private ProjectDto projectDto;

  @Before
  public void setup() {
    projectDto = new ProjectDto();
    projectDto.setCohortId(1L);
    projectDto.setCreateDate(OffsetDateTime.now());
    projectDto.setId(1L);

    UserDetails approvedCoordinator =
            UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();
  }

  @Test(expected = SystemException.class)
  public void findById() {
    when(cohortRepository.findById(2L))
            .thenThrow(new SystemException(ProjectDocCreator.class, CAN_T_FIND_THE_COHORT_BY_ID,
                            String.format(CAN_T_FIND_THE_COHORT_BY_ID, 2L)));
    cohortRepository.findById(2L);
  }

}
