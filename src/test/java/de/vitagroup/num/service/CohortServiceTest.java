package de.vitagroup.num.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CohortServiceTest {

  @InjectMocks private CohortService cohortService;

  @Mock private CohortRepository cohortRepository;

  @Mock private CohortExecutor cohortExecutor;

  @Before
  public void setup() {
    when(cohortRepository.findById(1L)).thenReturn(Optional.empty());
    when(cohortRepository.findById(2L)).thenReturn(Optional.of(Cohort.builder().id(2L).build()));
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingCohortWhenRetrieving() {
    cohortService.getCohort(1L);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingCohortWhenExecuting() {
    cohortService.executeCohort(1L);
  }

  @Test
  public void shouldExecuteCohortExactlyOnce() {
    cohortService.executeCohort(2L);
    verify(cohortExecutor, times(1)).execute(any());
  }

  @Test
  public void shouldExecuteCohortExactlyOnceWhenRetrievingSize() {
    cohortService.getCohortSize(2L);
    verify(cohortExecutor, times(1)).execute(any());
  }

  @Test
  public void shouldCallRepoWhenRetrievingAllCohorts() {
    cohortService.getAllCohorts();
    verify(cohortRepository, times(1)).findAll();
  }

}
