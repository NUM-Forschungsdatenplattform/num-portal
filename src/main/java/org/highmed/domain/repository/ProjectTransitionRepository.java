package org.highmed.domain.repository;

import org.highmed.domain.model.ProjectStatus;
import org.highmed.domain.model.ProjectTransition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTransitionRepository extends JpaRepository<ProjectTransition, Long> {

  Optional<List<ProjectTransition>> findAllByProjectIdAndFromStatusAndToStatus(
      Long projectId, ProjectStatus fromStatus, ProjectStatus toStatus);
}


