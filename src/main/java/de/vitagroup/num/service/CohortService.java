package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CohortService {

  private final CohortRepository cohortRepository;

  private final CohortExecutor cohortExecutor;

  public List<Cohort> getAllCohorts() {
    return cohortRepository.findAll();
  }

  public Cohort getCohort(Long cohortId) {
    return cohortRepository.findById(cohortId).orElseThrow(ResourceNotFound::new);
  }

  public Cohort createCohort(Cohort cohort) {
    return cohortRepository.save(cohort);
  }

  public Set<String> executeCohort(long cohortId) {
    Optional<Cohort> cohort = cohortRepository.findById(cohortId);
    return cohortExecutor.execute(cohort.orElseThrow(BadRequestException::new));
  }

  public long getCohortSize(long cohortId) {
    return executeCohort(cohortId).size();
  }
}
