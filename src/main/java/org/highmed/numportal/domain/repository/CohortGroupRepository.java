package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.CohortGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortGroupRepository extends JpaRepository<CohortGroup, Long> {

}
