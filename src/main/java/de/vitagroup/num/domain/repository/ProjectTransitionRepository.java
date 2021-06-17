package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.ProjectTransitions;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTransitionRepository extends JpaRepository<ProjectTransitions, Long> {

  Optional<List<ProjectTransitions>> findAllByProjectIdAndFromStatusAndToStatus(
      Long projectId, ProjectStatus fromStatus, ProjectStatus toStatus);
}


