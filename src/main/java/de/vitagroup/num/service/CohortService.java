package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import java.util.List;
import java.util.UUID;
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
        //TODO: execute cohort and remove mocked list
        //Optional<Cohort> cohort =  cohortRepository.findById(cohortId);
        //ehrBaseService.getPatientIds("SELECT e/ehr_id/value FROM EHR e df");

        return List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public long getCohortSize(long cohortId) {
        //TODO: execute cohort and remove mocked list
        //Optional<Cohort> cohort =  cohortRepository.findById(cohortId);
        //ehrBaseService.getPatientIds("SELECT e/ehr_id/value FROM EHR e df");
        return executeCohort(cohortId).size();
    }
}
