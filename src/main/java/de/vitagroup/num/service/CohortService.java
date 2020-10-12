package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.repository.CohortRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;

    public List<Cohort> getAllCohorts() {
        return cohortRepository.findAll();
    }

    public Cohort createCohort(Cohort cohort) {
        return cohortRepository.save(cohort);
    }
}
