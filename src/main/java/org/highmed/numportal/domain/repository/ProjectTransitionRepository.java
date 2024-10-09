package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.ProjectTransition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTransitionRepository extends JpaRepository<ProjectTransition, Long> {

  Optional<List<ProjectTransition>> findAllByProjectIdAndFromStatusAndToStatus(
      Long projectId, ProjectStatus fromStatus, ProjectStatus toStatus);
}


