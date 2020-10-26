package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CohortService {

  private final CohortRepository cohortRepository;

  private final EhrBaseService ehrBaseService;

  public List<Cohort> getAllCohorts() {
    return cohortRepository.findAll();
  }

  public Cohort createCohort(Cohort cohort) {
    return cohortRepository.save(cohort);
  }

  public List<String> executeCohort(long cohortId) {
    // TODO: execute cohort and remove mocked list
    // Optional<Cohort> cohort =  cohortRepository.findById(cohortId);
    return ehrBaseService.getPatientIds("SELECT e/ehr_id/value FROM EHR e");
  }

  public long getCohortSize(long cohortId) {
    return executeCohort(cohortId).size();
  }
}
