package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.model.CohortGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortGroupRepository extends JpaRepository<CohortGroup, Long> {

}
