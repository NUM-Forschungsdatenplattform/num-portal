package org.highmed.domain.repository;

import org.highmed.domain.model.CohortGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortGroupRepository extends JpaRepository<CohortGroup, Long> {

}
